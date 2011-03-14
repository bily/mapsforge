//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.11 at 03:25:14 PM MEZ 
//

package org.mapsforge.preprocessing.automization;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for source complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="source">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://mapsforge.org/mapsforge-preprocessing-conf}sink-source" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://mapsforge.org/mapsforge-preprocessing-conf}sink" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="file" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "source", propOrder = { "sinkSource", "sink" })
@XmlSeeAlso({ ReadPbf.class })
public abstract class Source {

	@XmlElementRef(name = "sink-source", namespace = "http://mapsforge.org/mapsforge-preprocessing-conf", type = JAXBElement.class)
	protected List<JAXBElement<? extends SinkSource>> sinkSource;
	@XmlElementRef(name = "sink", namespace = "http://mapsforge.org/mapsforge-preprocessing-conf", type = JAXBElement.class)
	protected List<JAXBElement<? extends Sink>> sink;
	@XmlAttribute(required = true)
	protected String file;

	public String generate() {
		int teeTotal = (this.sinkSource != null ? this.sinkSource.size() : 0)
				+ (this.sink != null ? this.sink.size() : 0);

		StringBuilder sb = new StringBuilder();
		if (teeTotal >= 2)
			sb.append("--tee").append(" ").append("outputCount=").append(teeTotal)
					.append(" ");

		if (this.sinkSource != null) {
			for (JAXBElement<? extends SinkSource> ss : this.sinkSource) {
				sb.append(ss.getValue().generate()).append(" ");
			}
		}

		if (this.sink != null) {
			for (JAXBElement<? extends Sink> s : this.sink) {
				sb.append(s.getValue().generate()).append(" ");
			}
		}
		return sb.toString();
	}

	/**
	 * Gets the value of the sinkSource property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
	 * modification you make to the returned list will be present inside the JAXB object. This
	 * is why there is not a <CODE>set</CODE> method for the sinkSource property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getSinkSource().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link JAXBElement }{@code <}
	 * {@link SinkSource }{@code >} {@link JAXBElement }{@code <}{@link BboxAreaFilter }{@code >}
	 * {@link JAXBElement }{@code <}{@link PolygonAreaFilter }{@code >}
	 * 
	 * 
	 */
	public List<JAXBElement<? extends SinkSource>> getSinkSource() {
		if (sinkSource == null) {
			sinkSource = new ArrayList<JAXBElement<? extends SinkSource>>();
		}
		return this.sinkSource;
	}

	/**
	 * Gets the value of the sink property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
	 * modification you make to the returned list will be present inside the JAXB object. This
	 * is why there is not a <CODE>set</CODE> method for the sink property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getSink().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link JAXBElement }{@code <}
	 * {@link Sink }{@code >} {@link JAXBElement } {@code <}{@link WritePbf }{@code >}
	 * {@link JAXBElement }{@code <} {@link MapfileWriter }{@code >} {@link JAXBElement }{@code <}
	 * {@link RoutinggraphWriter }{@code >}
	 * 
	 * 
	 */
	public List<JAXBElement<? extends Sink>> getSink() {
		if (sink == null) {
			sink = new ArrayList<JAXBElement<? extends Sink>>();
		}
		return this.sink;
	}

	/**
	 * Gets the value of the file property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getFile() {
		return file;
	}

	/**
	 * Sets the value of the file property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setFile(String value) {
		this.file = value;
	}

}
