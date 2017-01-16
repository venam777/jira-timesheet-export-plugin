package com.bftcom.timesheet.export;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.bftcom.timesheet.export.utils.Constants;
import com.bftcom.timesheet.export.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class FinanceProjectImporter {

    private static FinanceProjectImporter instance = new FinanceProjectImporter();
    private static Logger logger = LoggerFactory.getLogger(FinanceProjectImporter.class);

    public void startImport(String dirName) throws ParserConfigurationException, IOException, SAXException {
        logger.debug("import worklogs started");
        File dir = new File(dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.debug("dir exists = " + dir.exists());
            logger.debug("file is directory = " + dir.isDirectory());
            return;
        }
        for (String fileName : dir.list((dir1, name) -> name.startsWith("msg") && name.endsWith(".xml"))) {
            logger.debug("start handling file " + fileName);
            File fXmlFile = new File(dirName + fileName);
            if (!fXmlFile.exists()) {
                continue;
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource source = new InputSource(fXmlFile.toURI().toString());
            source.setEncoding(Settings.importEncoding);
            Document doc;
            doc = dBuilder.parse(source);
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();//rpl

            if (doc.hasChildNodes()) {
                Element el = (Element) root.getElementsByTagName("FINPROJECT").item(0);
                if (el == null) {
                    logger.error("tag FINPROJECT must be into root tag " + root.getTagName());
                    if (Settings.deleteFilesAfterImport) {
                        fXmlFile.delete();
                    }
                    continue;
                }
                el = (Element) el.getElementsByTagName("CHANGED").item(0);
                if (el == null) {
                    logger.error("tag CHANGED must be into tab FINPROJECT");
                }
                NodeList finprojects = el.getElementsByTagName("FINPROJECT");
                if (finprojects == null || finprojects.getLength() == 0) {
                    logger.error("There is no any finprojects into tag FINPROJECT");
                }
                CustomField financeProjectField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(Constants.financeProjectFieldName);
                if (financeProjectField == null) {
                    logger.error("Custom field with name = " + Constants.financeProjectFieldName + " was not found!");
                    continue;
                }
                for (int index = 0; index < finprojects.getLength(); index++) {
                    Node t = finprojects.item(index);
                    if (t.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) t;
                        String name = element.getAttribute("NAME");
                        String closed = element.getAttribute("ISCLOSE");
                        if (name != null && !name.equals("") && closed.equals("0")) {
                            String id = element.getAttribute("ID");
                            checkForFinanceProjectOption(financeProjectField, id, name);
                        }
                    }
                }
            }
//            if (Settings.deleteFilesAfterImport) {
//                logger.debug("deleting file " + fXmlFile.getName());
//                fXmlFile.delete();
//            }
        }
    }

    protected Option checkForFinanceProjectOption(CustomField customField, String id, String name) {
        logger.debug("Start checking finance project option, searching for id = " + id + ", name = " + name);
        Options options = ComponentAccessor.getOptionsManager().getOptions(customField.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
        String value = name + " #" + id;
        Option oldValue = null;
        for (Option option : options) {
            if (option.getValue() != null && option.getValue().endsWith(id)) {
                oldValue = option;
                break;
            }
        }
        if (oldValue != null) {
            if (!oldValue.getValue().equalsIgnoreCase(value) && !oldValue.getValue().startsWith(name)) {
                disableOption(oldValue);
                return addOptionToCustomField(customField, value);
            } else {
                logger.debug("Value was found");
                return oldValue;
            }
        } else {
            logger.debug("Value was not found, need to create value");
            return addOptionToCustomField(customField, value);
        }
    }

    public Option addOptionToCustomField(CustomField customField, String value) {
        logger.debug("Start create new Option value for combobox, value: " + value);
        Option newOption = null;
        if (customField != null) {
            List<FieldConfigScheme> schemes = customField
                    .getConfigurationSchemes();
            if (schemes != null && !schemes.isEmpty()) {
                FieldConfigScheme sc = schemes.get(0);
                Map configs = sc.getConfigsByConfig();
                if (configs != null && !configs.isEmpty()) {
                    FieldConfig config = (FieldConfig) configs.keySet()
                            .iterator().next();

                    OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
                    Options l = optionsManager.getOptions(config);
                    newOption = optionsManager.createOption(config, null, Long.valueOf(l.size() + 1), value);
                }
            }
        }
        logger.debug("Finished create new Option for combobox");
        return newOption;
    }

    public void disableOption(Option option) {
        logger.debug("Start disabling Option value for combobox, value: " + option.getValue());
        OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
        optionsManager.disableOption(option);
        logger.debug("Finished disabling Option for combobox");
    }

    public static FinanceProjectImporter getInstance() {
        return instance;
    }
}
