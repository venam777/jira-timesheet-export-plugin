package com.bftcom.timesheet.export;

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

public class WorklogImporter {

    private WorklogDataDao dao;
    private static WorklogImporter instance;
    private String encoding = "windows-1251";
//    private String encoding = "UTF-8";
    private static Logger logger = LoggerFactory.getLogger(WorklogImporter.class);

    private WorklogImporter(WorklogDataDao dao) {
        this.dao = dao;
    }

    public void importWorklog(String dirName) throws ParserConfigurationException, IOException, SAXException {
        logger.debug("import worklogs started");
        File dir = new File(dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.debug("dir exists = " + dir.exists());
            logger.debug("file is directory = " + dir.isDirectory());
            return;
        }
        for (String fileName : dir.list((dir1, name) -> name.startsWith("msg") && name.endsWith(".xml"))) {
            logger.debug("start handling file " + fileName);
            File fXmlFile = new File(dirName +  fileName);
            if (!fXmlFile.exists()) {continue;}
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource source = new InputSource(fXmlFile.toURI().toString());
            source.setEncoding(encoding);
            Document doc = dBuilder.parse(source);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();//rpl

            if (doc.hasChildNodes()) {
                Element el = (Element) root.getElementsByTagName("TIMESHEET").item(0);
                if (el == null) {
                    logger.error("tag TIMESHEET must be into root tag " + root.getTagName());
                    if (Settings.deleteFilesAfterImport) {
                        fXmlFile.delete();
                    }
                    continue;
                }
                el = (Element) el.getElementsByTagName("CHANGED").item(0);
                if (el == null) {
                    logger.error("tag CHANGED must be into tab TIMESHEET");
                }
                NodeList timesheets = el.getElementsByTagName("TIMESHEET");
                if (timesheets == null || timesheets.getLength() == 0) {
                    logger.error("There is no any timesheets into tag TIMESHEET");
                }
                for (int index = 0; index < timesheets.getLength(); index++) {
                    Node t = timesheets.item(index);
                    if (t.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) t;
                        logger.debug("parsing node with parameters: id = " + element.getAttribute("ID")
                        + ", status = " + element.getAttribute("STATUS") + ", reject comment = " + element.getAttribute("REJECT_COMMENT"));
                        Long id = Long.valueOf(element.getAttribute("ID"));
                        String status = element.getAttribute("STATUS");
                        String rejectComment = element.getAttribute("REJECT_COMMENT");
                        dao.update(id, status, rejectComment);
                    }
                }
            }
            if (Settings.deleteFilesAfterImport) {
                logger.debug("deleting file " + fXmlFile.getName());
                fXmlFile.delete();
            }
        }
    }

    public synchronized static void createInstance(WorklogDataDao dao) {
        instance = new WorklogImporter(dao);
    }

    public synchronized static WorklogImporter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Instance must be created by method createInstance(dao) before using!");
        }
        return instance;
    }
}
