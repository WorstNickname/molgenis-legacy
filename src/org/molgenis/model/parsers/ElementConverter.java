package org.molgenis.model.parsers;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;

public class ElementConverter {

    public static String elementValueToString(Element element) {
        StringWriter writer = new StringWriter();

        OutputFormat format = new OutputFormat(element.getOwnerDocument());
        format.setIndenting(true);
        format.setOmitXMLDeclaration(true);

        XMLSerializer serializer = new XMLSerializer(writer, format);
        try {
            serializer.serialize(element);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String xml = writer.toString();
        xml = xml.replace("<" + element.getTagName() + ">", "");
        xml = xml.replace("</" + element.getTagName() + ">", "");
        xml = xml.replace("<" + element.getTagName() + "/>", "");

        return xml;
    }
}
