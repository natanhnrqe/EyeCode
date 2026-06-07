package com.eyecode.maven;



import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class PomParser {

    public MavenProject parse(File pomFile) {

        MavenProject project = new MavenProject();

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(pomFile);

            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();

            project.setGroupId(getTagValue(root, "groupId"));
            project.setArtifactId(getTagValue(root, "artifactId"));
            project.setVersion(getTagValue(root, "version"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return project;
    }

    private String getTagValue(
            Element parent,
            String tag
    ) {

        NodeList list =
                parent.getElementsByTagName(tag);

        if (list.getLength() == 0) {
            return null;
        }

        return list.item(0)
                .getTextContent()
                .trim();
    }
}
