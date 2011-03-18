//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.11 at 03:25:14 PM MEZ 
//

package org.mapsforge.preprocessing.automization;

import java.io.File;
import java.util.List;

import javax.imageio.IIOException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for write-pbf complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="write-pbf">
 *   &lt;complexContent>
 *     &lt;extension base="{http://mapsforge.org/mapsforge-preprocessing-conf}sink">
 *       &lt;attribute name="omit-metadata" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *        &lt;attribute name="compress" default="none">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="none"/>
 *             &lt;enumeration value="deflate"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "write-pbf")
public class WritePbf
		extends Sink {

	/**
	 * This parameter defines if the osm meta data should be transfered into the written pbf
	 * file or if the are filtered.
	 */
	@XmlAttribute(name = "omit-metadata")
	private Boolean omitMetadata;

	/**
	 * This parameter defines if the deflate or no compression method should use for writing the
	 * pbf file.
	 */
	@XmlAttribute
	private String compress;

	/**
	 * Gets the value of the omitMetadata property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public boolean isOmitMetadata() {
		if (omitMetadata == null) {
			return false;
		}
		return omitMetadata;
	}

	/**
	 * Sets the value of the omitMetadata property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	public void setOmitMetadata(Boolean value) {
		this.omitMetadata = value;
	}

	/**
	 * Gets the value of the compress property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getCompress() {
		if (compress == null) {
			return "none";
		}
		return compress;
	}

	/**
	 * Sets the value of the compress property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setCompress(String value) {
		this.compress = value;
	}

	@Override
	public String generate(List<String> md5List, String absolutePath) {

		StringBuilder sb = new StringBuilder();
		File outputFile;

		// check if the path of the file is absolute
		if (getFile().startsWith(File.separator)) {
			// file is absolute
			outputFile = new File(getFile());
		} else {
			// file is not absolute, so it must be combined with the absolute path of the output
			// directory
			outputFile = new File(absolutePath, getFile());
		}

		// check if path exists

		try {
			if (!outputFile.exists())
				if (!outputFile.getParentFile().exists())
					if (outputFile.getParentFile().canWrite()) {
						if (!outputFile.getParentFile().mkdirs())
							throw new IIOException("cannot create path: "
									+ outputFile.getParent());
					} else
						throw new IIOException("cannot write path: "
									+ outputFile.getParent());
		} catch (IIOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

		if (isMd5()) {
			md5List.add(outputFile.getAbsolutePath());
		}

		sb.append("--wb").append(" ");
		sb.append("file=").append(outputFile.getAbsolutePath()).append(" ");

		if (compress != null)
			sb.append("compress=").append("deflate").append(" ");
		if (omitMetadata != null)
			sb.append("omitmetadata=").append(omitMetadata).append(" ");

		return sb.toString();
	}
}
