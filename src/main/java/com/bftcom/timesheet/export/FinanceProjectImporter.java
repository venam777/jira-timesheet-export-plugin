package com.bftcom.timesheet.export;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
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
    private static String financeProjectFieldName = "Бюджет проекта ПУ";

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
                CustomField financeProjectField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(financeProjectFieldName);
                if (financeProjectField == null) {
                    logger.error("Custom field with name = " + financeProjectFieldName + " was not found!");
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
                            String newValue = name + " #" + id;
                            checkForCustomFieldValue(financeProjectField.getIdAsLong(), newValue);
                        }
                    }
                }
            }
            if (Settings.deleteFilesAfterImport) {
                logger.debug("deleting file " + fXmlFile.getName());
                fXmlFile.delete();
            }
        }
    }

    protected Option checkForCustomFieldValue(Long customFieldId, String value) {
        logger.debug("Start checking combobox values, searching for: " + value);
        CustomField cf = ComponentAccessor.getComponent(CustomFieldManager.class).getCustomFieldObject(customFieldId);
        Options options = ComponentAccessor.getOptionsManager().getOptions(cf.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
        if (options.getOptionForValue(value, null) == null) {
            logger.debug("Value was not found, need to create value");
            return addOptionToCustomField(cf, value);
        }
        logger.debug("Value was found");
        return options.getOptionForValue(value, null);
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
                    newOption = optionsManager.createOption(config, null,
                            Long.valueOf(l.size() + 1), // TODO What is this
                            value);
                }
            }
        }
        logger.debug("Finished create new Option for combobox");
        return newOption;
    }

    public static FinanceProjectImporter getInstance() {
        return instance;
    }
}
