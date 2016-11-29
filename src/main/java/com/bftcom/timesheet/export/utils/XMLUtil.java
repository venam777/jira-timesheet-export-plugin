package com.bftcom.timesheet.export.utils;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.dom.DOMInputImpl;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.XMLVersionDetector;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xerces.parsers.DOMParserImpl;
import org.apache.xerces.parsers.XML11Configuration;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: a.gshyan
 * Date: 02.02.15
 * Time: 17:21
 * To change this template use File | Settings | File Templates.
 */
public class XMLUtil {

        /**
         * @param element элемент
         * @param name    имя атрибута
         * @return значение атрибута или null, если не существует.
         */
        public static String getAttr(Element element, String name) {
            return element.hasAttribute(name) ? element.getAttribute(name) : null;
        }


        /**
         * Возвращает форматированный текст из элемента
         *
         * @param el элемент, содержимое которого будет возвращено в виде текста
         * @return форматированный текст
         */
        public static String getFormattedXMLText(Element el, String encoding) {
            StringWriter stringOut = new StringWriter();
            getFormattedXMLText(el, stringOut, encoding);
            return stringOut.toString();
        }

        public static String getFormattedXMLText(final Element el) {
            StringWriter stringOut = new StringWriter();
            getFormattedXMLText(el, stringOut, null);
            return stringOut.toString();
        }

        public static Element createChildElement(Element parent, String nodeName) {
            final Element result = parent.getOwnerDocument().createElement(nodeName);
            parent.appendChild(result);
            return result;
        }

        public static void getFormattedXMLText(Element el, Writer writer, String encoding) {
            final Properties props = new Properties();
            props.setProperty(OutputKeys.INDENT, "yes");
            getXMLText(el, writer, props, encoding);
        }

        /**
         * Помещает XML в OutputStream форматированный xml
         */
        public static void getFormattedXMLText(Element el, OutputStream outputStream) {
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(outputStream, CharEncoding.UTF_8);
                getFormattedXMLText(el, writer, null);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Помещает XML в OutputStream форматированный xml
         */
        public static void getFormattedXMLText(Element el, OutputStream outputStream, String encoding) {
            Charset charset = Charset.forName(encoding);
            getFormattedXMLText(el, outputStream, charset);
        }

        public static void getFormattedXMLText(Element el, OutputStream outputStream, Charset charset) {
            Writer writer = new OutputStreamWriter(outputStream, charset);
            getFormattedXMLText(el, writer, charset.name());
        }

        /**
         * Возвращает неформатированный текст
         */
        public static String getXMLText(final Element el) {
            StringWriter stringOut = new StringWriter();
            getXMLText(el, stringOut, null);
            return stringOut.toString();
        }

        /**
         * Возвращает неформатированный текст
         */
        public static String getXMLText(final Element el, String encoding) {
            StringWriter stringOut = new StringWriter();
            getXMLText(el, stringOut, encoding);
            return stringOut.toString();
        }

        /**
         * Помещает XML в writer неформатированный xml
         */
        public static void getXMLText(Element el, Writer writer, String encoding) {
            getXMLText(el, writer, null, encoding);
        }

        /**
         * Помещает XML в OutputStream не форматированный xml
         */
        public static void getXMLText(Element el, OutputStream outputStream) {
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(outputStream, CharEncoding.UTF_8);
                getXMLText(el, writer, null);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Помещает XML в OutputStream не форматированный xml
         */
        public static void getXMLText(Element el, OutputStream outputStream, String encoding) {
            Writer writer;
            try {
                writer = new OutputStreamWriter(outputStream, encoding);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            getXMLText(el, writer, encoding);
        }

        public static void getXMLText(Element el, Writer writer, Properties properties, String encoding) {
            try {
                Properties props = (properties == null) ? new Properties() : properties;
                if (encoding != null) {
                    props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    writer.write("<?xml version=\"" + el.getOwnerDocument().getXmlVersion() + "\" encoding=\"");
                    writer.write(encoding);
                    writer.write("\" ?>\r\n");
                }

                Document doc = el.getOwnerDocument();
                DOMSource domSource = new DOMSource(doc);
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                if (properties != null) {
                    transformer.setOutputProperties(props);
                }
                transformer.transform(domSource, result);
                writer.flush();
            } catch (TransformerException e) {
                throw new UnsupportedOperationException(e);
            } catch (IOException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        /**
         * Возвращает первый элемент с именем tagName вложенный в элемент el
         * Поиск осуществляется только на одном уровне вложенности
         *
         * @param el      элемент внутри которого осуществляется поиск
         * @param tagName имя искомого элемента
         * @return найденный элемент
         * @throws IllegalArgumentException если ничего не найдено.
         */
        public static Element getChildElement(Element el, String... tagName) {
            Element childElement = findChildElement(el, tagName);
            if (childElement == null) {
                throw new IllegalArgumentException("Не найден элемент " + Arrays.asList(tagName).toString());
            }
            return childElement;
        }

        public static Element findChildElement(Element el, String... tagName) {
            Element currEl = el;
            Element childElement;
            for (String name : tagName) {
                childElement = getFirstChildElement(currEl, name);
                if (childElement == null) {
                    return null;
                }
                currEl = childElement;
            }
            return currEl;
        }

        private static XMLHelper helper = new XMLHelper();

        public static class XMLConfig extends XML11Configuration {
            public XMLConfig() {
                super();
                fVersionDetector = new XMLAlways11VersionDetector();
            }

            public static class XMLAlways11VersionDetector extends XMLVersionDetector {
                public XMLAlways11VersionDetector() {
                    super();
                }

                public short determineDocVersion(XMLInputSource inputSource) throws IOException {
                    super.determineDocVersion(inputSource);
                    return Constants.XML_VERSION_1_1;
                }
            }
        }

        /**
         * Копирует все атрибуты переданного елемента и его внутренние элементы
         *
         * @param source источник
         * @param dest   пункт назначения
         */
        private static void copyElementContentSameOwner(Element source, Element dest) {
            NamedNodeMap nnm = source.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                dest.setAttribute(nnm.item(i).getNodeName(), nnm.item(i).getNodeValue());
            }

            // теперь копируем все внутренние элементы
            Node node = source.getFirstChild();
            while (node != null) {
                dest.appendChild(node.cloneNode(true));
                node = node.getNextSibling();
            }
        }

        private static void copyElementContent(Element source, Element dest) {
            NamedNodeMap nnm = source.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                dest.setAttribute(nnm.item(i).getNodeName(), nnm.item(i).getNodeValue());
            }

            // теперь копируем все внутренние элементы
            Node node = source.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE || node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                    if (node.getNodeType() == Node.TEXT_NODE) {
                        Text el = dest.getOwnerDocument().createTextNode(node.getNodeValue());
                        dest.appendChild(el);
                    } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element el = dest.getOwnerDocument().createElement(node.getNodeName());
                        dest.appendChild(el);
                        copyElementContent((Element) node, el);
                    } else {
                        CDATASection el = dest.getOwnerDocument().createCDATASection(node.getNodeValue());
                        dest.appendChild(el);
                    }
                }
                node = node.getNextSibling();
            }
        }

        /**
         * Клонирует елемент source
         *
         * @param source источник
         * @return клонированный элемент
         */
        public static Element cloneElement(Element source) {
            if (source == null) {
                throw new IllegalArgumentException("Source is null");
            }
            return cloneElement(source, source.getTagName());
        }

        /**
         * Клонирует елемент source со сменой имени тега
         *
         * @param source  источник
         * @param tagName новое имя тега нового елемента,
         * @return клонированный элемент
         */
        public static Element cloneElement(Element source, String tagName) {
            Element newEl = source.getOwnerDocument().createElement(tagName);
            copyElementContentSameOwner(source, newEl);
            return newEl;
        }

        public static void copyElementInTo(Element source, Element dst) {
            Element elNew = dst.getOwnerDocument().createElement(source.getTagName());
            dst.appendChild(elNew);
            if (source.getOwnerDocument() == dst.getOwnerDocument()) {
                copyElementContentSameOwner(source, elNew);
            } else {
                copyElementContent(source, elNew);
            }
        }

        public static Document createDocument() {
            return helper.newDocument();
        }

        public static Element createDocumentElement(String tagName) {
            Document document = helper.newDocument();
            Element element = document.createElement(tagName);
            document.appendChild(element);
            return element;
        }

        public static Document createDocument(String xml) {
            LSParser parser = helper.getParser();
            try {
                LSInput input = helper.createLSInput();
                input.setStringData(xml);
                return parser.parse(input);
            } finally {
                helper.putParser(parser);
            }
        }

        public static Document createDocument(Reader xmlReader) {
            LSParser parser = helper.getParser();
            try {
                LSInput input = helper.createLSInput();
                input.setCharacterStream(xmlReader);
                return parser.parse(input);
            } finally {
                helper.putParser(parser);
            }
        }

        /**
         * Загружает содержимое XML файла и разбирает его. Файл должен быть в кодировке
         * <?xml encoding=".." ..>
         *
         * @param file загружаемый файл
         * @return разобранный Document
         * @throws IOException
         */
        public static Document createDocument(File file) throws IOException {
            return createDocument(new BufferedInputStream(new FileInputStream(file)));
        }

        /**
         * Загружает содержимое из бинарного потока. Поток должен быть в кодировке
         * <?xml encoding=".." ..>
         *
         * @param in входящий поток
         * @return разобранный Document
         */
        public static Document createDocument(final InputStream in) {
            LSParser parser = helper.getParser();
            try {
                LSInput input = helper.createLSInput();
                input.setByteStream(in);
                return parser.parse(input);
            } finally {
                helper.putParser(parser);
            }
        }

        /**
         * Работает аналогично createDocument(InputStream in), но устанавливается
         * кодировка потока.
         *
         * @param encoding кодировка в которой будет читаться поток
         */
        public static Document createDocument(final InputStream in, String encoding) {
            LSParser parser = helper.getParser();
            try {
                LSInput input = helper.createLSInput();
                input.setByteStream(in);
                input.setEncoding(encoding);
                return parser.parse(input);
            } finally {
                helper.putParser(parser);
            }
        }

        /**
         * Работает аналогично createDocument(File file), но устанавливается
         * кодировка в которой будет читаться файл.
         *
         * @param encoding кодировка в которой будет читаться файл.
         */
        public static Document createDocument(File file, String encoding)
                throws FileNotFoundException
        {
            return createDocument(new FileInputStream(file), encoding);
        }

        /**
         * Returns map of element attributes
         *
         * @param el element
         * @return map of all attributes
         */
        public static Map<String, String> getElementAttributes(Element el) {
            final Map<String, String> map = new HashMap();
            final NamedNodeMap nodes = el.getAttributes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node node = nodes.item(i);
                map.put(node.getNodeName(), node.getNodeValue());
            }
            return map;
        }

        /**
         * Возвращает текст элемента
         *
         * @param el элемент текст которого извлекается
         * @return текст внутри элемента, null если текст не обнаружен
         */
        public static String getElementText(Element el) {
            Node n = el.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.TEXT_NODE) {
                    return ((Text) n).getData();
                }
                n = n.getNextSibling();
            }
            return null;
        }

        /**
         * Возвращает текст из первого CDATA элемента
         *
         * @param el элемент текст CDATA которого извлекается
         * @return текст CDATA внутри элемента; null если текст не обнаружен
         */
        public static String getElementFirstCData(Element el) {
            Node n = el.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
                    return ((Text) n).getData();
                }
                n = n.getNextSibling();
            }
            return null;
        }

        /**
         * Возвращает текст CDATA элемента. Если в элементе несколько CDATA, объединяет их тела.
         *
         * @param element элемент текст CDATA которого извлекается
         * @return текст внутри элемента; null если текст не обнаружен или все CDATA пусты
         */
        public static String getElementCDATA(Element element) {
            StringBuffer buffer = new StringBuffer();
            Node n = element.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
                    buffer.append(((CDATASection) n).getData());
                }
                n = n.getNextSibling();
            }
            return StringUtils.trimToNull(buffer.toString());
        }

        /**
         * Добавляет к <code>el CDATA</code> секцию
         *
         * @param el    - элемент, к которому добавляется <code>CDATASection</code>
         * @param value - значиене <code>CDATA</code> секции
         */
        public static void setElementCData(Element el, String value) {
            CDATASection cdata = el.getOwnerDocument().createCDATASection(strToCDATAStr(value));
            el.appendChild(cdata);
        }

        /**
         * Убирает символ \r из строки
         *
         * @param params - строка, откуда необходимо убрать символ \r
         * @return строка, с убранными символами \r
         */
        public static String strToCDATAStr(String params) {
            return StringUtils.isNotEmpty(params) ? params.replaceAll("\r", "") : params;
        }

        /**
         * Возвращает первый элемент с именем tagName вложенный в элемент el
         * Поиск осуществляется только на одном уровне вложенности
         *
         * @param el      элемент внутри которого осуществляется поиск
         * @param tagName имя искомого элемента
         * @return найденный элемент, null если ничего не найдено
         */
        public static Element getFirstChildElement(Element el, String tagName) {
            Node node = el.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE
                        && tagName.equals(node.getNodeName()))
                {
                    return (Element) node;
                }
                node = node.getNextSibling();
            }
            return null;
        }


        /**
         * Возвращает первый элемент с именем tagName вложенный в элемент el
         * Поиск осуществляется только на одном уровне вложенности
         *
         * @param el элемент внутри которого осуществляется поиск
         * @return найденный элемент, null если ничего не найдено
         */
        public static Element getFirstChildElement(Element el) {
            Node node = el.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    return (Element) node;
                }
                node = node.getNextSibling();
            }
            return null;
        }


        /**
         * Возвращает первый элемент с именем tagName находящийся ниже элемента el
         * Поиск осуществляется только на уровне элемента el
         *
         * @param el      элемент на уровене которого осуществляется поиск
         * @param tagName имя искомого элемента
         * @return найденный элемент, null если ничего не найдено
         */
        public static Element getNextSiblingElement(Element el, String tagName) {
            Node node = el.getNextSibling();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE
                        && tagName.equals(node.getNodeName()))
                {
                    return (Element) node;
                }
                node = node.getNextSibling();
            }
            return null;
        }

        /**
         * Возвращает первый элемент с именем tagName находящийся ниже элемента el
         * Поиск осуществляется только на уровне элемента el
         *
         * @param el элемент на уровене которого осуществляется поиск
         * @return найденный элемент, null если ничего не найдено
         */
        public static Element getNextSiblingElement(Element el) {
            Node node = el.getNextSibling();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    return (Element) node;
                }
                node = node.getNextSibling();
            }
            return null;
        }


        public static void copyElement(Element source, Element dst) {
            if (source.getOwnerDocument() == dst.getOwnerDocument()) {
                copyElementContentSameOwner(source, dst);
            } else {
                copyElementContent(source, dst);
            }
        }


        /**
         * Копирует атрибуты из source в dest
         *
         * @param source - source element
         * @return - dest element
         */
        public static Element copyElementAttr(Element source) {
            Element dest = source.getOwnerDocument().createElement(source.getTagName());
            NamedNodeMap nnm = source.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                dest.setAttribute(nnm.item(i).getNodeName(), nnm.item(i).getNodeValue());
            }
            return dest;
        }

        private static final class XMLHelper {
            private static final int MAX_SIZE = 10;
            private final LSParser[] parsers = new LSParser[MAX_SIZE];
            private int index = -1;

            private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY =
                    DocumentBuilderFactory.newInstance(DocumentBuilderFactoryImpl.class.getName(), XMLHelper.class.getClassLoader());

            private XMLHelper() {
            }

            public LSParser getParser() {
                synchronized (this) {
                    if (index >= 0) {
                        return parsers[index--];
                    }
                }
                XMLConfig xmlConfig = new XMLConfig();
                DOMParserImpl parser = new DOMParserImpl(xmlConfig);
                parser.setParameter(Constants.DOM_DATATYPE_NORMALIZATION, Boolean.FALSE);
                parser.setParameter(Constants.DOM_CDATA_SECTIONS, Boolean.TRUE);
                parser.setParameter(Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE, false);
                parser.setParameter(Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE, false);

                return parser;
            }

            public LSInput createLSInput() {
                return new DOMInputImpl();  //альтернатива return domImpl.createLSInput();
            }

            public Document newDocument() {
                Document doc = null;
                try {
                    doc = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().newDocument();
                    doc.setXmlVersion("1.1");
                } catch (ParserConfigurationException e) {
                    throw new IllegalArgumentException(e);
                }
                return doc;
            }

            public void putParser(LSParser p) {
                if (p.getBusy()) {
                    // Если в парсере произойдет ошибка, но не производная от Exception, то
                    // флаг busy(который показывает, что парсер занят в другом потоке)
                    // не будет сброшен, и он будет выдавать ошибку в последующие
                    // вызовы parse()
                    return;
                }
                synchronized (this) {
                    if (index < MAX_SIZE) {
                        parsers[++index] = p;
                    }
                }
            }
        }

}


