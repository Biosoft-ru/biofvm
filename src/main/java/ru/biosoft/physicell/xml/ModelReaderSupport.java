package ru.biosoft.physicell.xml;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ModelReaderSupport extends Constants
{
    public Element findElement(Element parent, String tag)
    {
        return findElement( parent.getChildNodes(), tag );
    }

    public Element findElement(NodeList list, String tag)
    {
        for( int i = 0; i < list.getLength(); i++ )
        {
            Node node = list.item( i );
            if( node instanceof Element && ( (Element)node ).getTagName().equals( tag ) )
            {
                return (Element)node;
            }
        }
        return null;
    }

    public List<Element> getAllElements(Element parent)
    {
        List<Element> result = new ArrayList<>();
        NodeList list = parent.getChildNodes();
        for( int i = 0; i < list.getLength(); i++ )
        {
            Node node = list.item( i );
            if( node instanceof Element )
            {
                result.add( (Element)node );
            }
        }
        return result;
    }

    public List<Element> findAllElements(Element parent, String tag)
    {
        List<Element> result = new ArrayList<>();
        for( int i = 0; i < parent.getChildNodes().getLength(); i++ )
        {
            Node node = parent.getChildNodes().item( i );
            if( node instanceof Element && ( (Element)node ).getTagName().equals( tag ) )
            {
                result.add( (Element)node );
            }
        }
        return result;
    }


    public boolean getBoolVal(Element el)
    {
        return Boolean.parseBoolean( getVal( el ) );
    }

    public String getVal(Element el)
    {
        return el.getChildNodes().item( 0 ).getNodeValue();// el.getNodeValue();
    }

    public double getDoubleVal(Element el)
    {
        return Double.parseDouble( getVal( el ) );
    }

    public int getIntVal(Element el)
    {
        return Integer.parseInt( getVal( el ) );
    }

    public double getDoubleAttr(Element el, String name)
    {
        return Double.parseDouble( el.getAttribute( name ) );
    }

    public boolean getBoolAttr(Element el, String name)
    {
        return Boolean.parseBoolean( el.getAttribute( name ) );
    }

    public String getAttr(Element el, String name)
    {
        return el.getAttribute( name );
    }

    public boolean hasAttr(Element el, String name)
    {
        return el.hasAttribute( name );
    }

    public Integer getIntAttr(Element el, String name)
    {
        return Integer.parseInt( el.getAttribute( name ) );
    }

    public static Color readColor(String str)
    {
        switch( str )
        {
            case "green":
                return Color.green;
            case "red":
                return Color.red;
            case "blue":
                return Color.blue;
            case "limegreen":
                return new Color( 50, 205, 50 );
            case "magenta":
                return Color.magenta;
            case "cyan":
                return Color.cyan;
            default:
                return Color.white;
        }
    }
}
