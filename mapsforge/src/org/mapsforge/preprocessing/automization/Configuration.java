//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.11 at 03:25:14 PM MEZ 
//

package org.mapsforge.preprocessing.automization;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for configuration complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="configuration">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="pipeline" type="{http://mapsforge.org/mapsforge-preprocessing-conf}pipeline" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="osmosis-home" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="workspace" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="output-dir" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "configuration", propOrder = {
		"pipeline"
})
public class Configuration {

	/**
	 * A list of all pipelines that should run in this configuration.
	 */
	@XmlElement(required = true)
	protected List<Pipeline> pipeline;

	/**
	 * The path to the osmosis home directory.
	 */
	@XmlAttribute(name = "osmosis-home", required = true)
	protected String osmosisHome;

	/**
	 * The path to the workspace directory. This is the place where the configuration data
	 * (logs, files, scripts, etc.) would be stored.
	 */
	@XmlAttribute(required = true)
	protected String workspace;

	/**
	 * The path to the directory where the generated content of the script should be stored.
	 */
	@XmlAttribute(name = "output-dir", required = true)
	protected String outputDir;

	/**
	 * Gets the value of the pipeline property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
	 * modification you make to the returned list will be present inside the JAXB object. This
	 * is why there is not a <CODE>set</CODE> method for the pipeline property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getPipeline().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Pipeline }
	 * 
	 * @return a list of all pipelines.
	 * 
	 * 
	 */
	public List<Pipeline> getPipeline() {
		if (pipeline == null) {
			pipeline = new ArrayList<Pipeline>();
		}
		return this.pipeline;
	}

	/**
	 * Gets the value of the osmosisHome property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getOsmosisHome() {
		return osmosisHome;
	}

	/**
	 * Sets the value of the osmosisHome property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setOsmosisHome(String value) {
		this.osmosisHome = value;
	}

	/**
	 * Gets the value of the workspace property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getWorkspace() {
		return workspace;
	}

	/**
	 * Sets the value of the workspace property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setWorkspace(String value) {
		this.workspace = value;
	}

	/**
	 * Gets the value of the outputDir property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getOutputDir() {
		return outputDir;
	}

	/**
	 * Sets the value of the outputDir property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setOutputDir(String value) {
		this.outputDir = value;
	}

}