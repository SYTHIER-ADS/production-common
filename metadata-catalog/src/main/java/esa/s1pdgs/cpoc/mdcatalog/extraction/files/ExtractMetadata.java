package esa.s1pdgs.cpoc.mdcatalog.extraction.files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.util.StringUtils;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataExtractionException;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataMalformedException;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.mdcatalog.extraction.WVFootPrintExtension;
import esa.s1pdgs.cpoc.mdcatalog.extraction.model.ConfigFileDescriptor;
import esa.s1pdgs.cpoc.mdcatalog.extraction.model.EdrsSessionFileDescriptor;
import esa.s1pdgs.cpoc.mdcatalog.extraction.model.OutputFileDescriptor;

/**
 * Class to extract the metadata from various types of files
 * 
 * @author Olivier Bex-Chauvet
 */
public class ExtractMetadata {

	private static final String PASS_ASC = "ASCENDING";
	private static final String PASS_DFT = "DESCENDING";
	private static final String XSLT_MPL_EOF = "XSLT_MPL_EOF.xslt";
	private static final String XSLT_AUX_EOF = "XSLT_AUX_EOF.xslt";
	private static final String XSLT_AUX_XML = "XSLT_AUX_XML.xslt";
	private static final String XSLT_AUX_MANIFEST = "XSLT_AUX_MANIFEST.xslt";
	private static final String XSLT_L0_MANIFEST = "XSLT_L0_MANIFEST.xslt";
	private static final String XSLT_L0_SEGMENT_MANIFEST = "XSLT_L0_SEGMENT.xslt";
	private static final String XSLT_L1_MANIFEST = "XSLT_L1_MANIFEST.xslt";
	private static final String XSLT_L2_MANIFEST = "XSLT_L2_MANIFEST.xslt";
	private static final String OUTPUT_XML = "tmp/output.xml";
	private static final String OUTPUT_L0_SEGMENT_XML = "tmp/outputl0seg.xml";
	
	private final Map<ProductFamily, String> xsltMap;

	/**
	 * XSLT transformer factory
	 */
	private TransformerFactory transFactory;

	/**
	 * Map of all the overlap for the different slice type
	 */
	private Map<String, Float> typeOverlap;

	/**
	 * Map of all the length for the different slice type
	 */
	private Map<String, Float> typeSliceLength;

	private String xsltDirectory;

	/**
	 * Constructor
	 */
	public ExtractMetadata(Map<String, Float> typeOverlap, Map<String, Float> typeSliceLength, String xsltDirectory) {
		this.transFactory = TransformerFactory.newInstance();
		this.typeOverlap = typeOverlap;
		this.typeSliceLength = typeSliceLength;
		this.xsltDirectory = xsltDirectory;
		
		this.xsltMap = new HashMap<>();
		this.xsltMap.put(ProductFamily.L0_ACN, XSLT_L0_MANIFEST);
		this.xsltMap.put(ProductFamily.L0_SLICE, XSLT_L0_MANIFEST);
		this.xsltMap.put(ProductFamily.L1_ACN, XSLT_L1_MANIFEST);
		this.xsltMap.put(ProductFamily.L1_SLICE, XSLT_L1_MANIFEST);
		this.xsltMap.put(ProductFamily.L2_ACN, XSLT_L2_MANIFEST);
		this.xsltMap.put(ProductFamily.L2_SLICE, XSLT_L2_MANIFEST);
	}

	/**
	 * Tool function which returns the content of a file
	 * 
	 * @param FileName
	 * @param encoding
	 * @return the content of the file
	 * @throws IOException
	 */
	private String readFile(String fileName, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(fileName));
		return new String(encoded, encoding);
	}

	/**
	 * Function which transform the raw coordinates in the good format
	 * 
	 * @param rawCoordinates
	 * @param productName
	 * @return the coordinates in good format
	 * @throws MetadataExtractionException
	 */
	private JSONObject processCoordinates(File manifest,OutputFileDescriptor descriptor, String rawCoordinates, String pass)
			throws MetadataExtractionException {
		
//        <xsl:when test="($productType='WV_OCN__2S') or ($productType='WV_SLC__1S') or ($productType = 'WV_GRDM_1S')">
		String productType = descriptor.getProductType();
		JSONObject geoShape = new JSONObject();
		JSONArray coordinates = new JSONArray();
		
//		if(productType.matches(".._RAW__0.")) {
//			if (productType.startsWith("WV")) {
//				//only 2 Nadir-Points in manifest --> Howto handle?
//			} else {
//				//should be 4 coordinates --> else exception
//				//copy from manifest -->  manifest is counterclockwise
//			}
//			
//		} else if(productType.matches(".._(GRD|SLC)._1.")) {
//			//L1
//			if (productType.startsWith("WV"))
//				{
//				//WV L1:
//				//derive larger footprint from multiple smaller patches 
//					return WVFootPrintExtension.getBoundingPolygon(manifest.getAbsolutePath());
//				} else {
//					//should be 4 coordinates --> else exception
//					//copy from manifest -->  manifest is counterclockwise
//				}
//		} else if(productType.matches(".._OCN__2.")) {
//			//l2
//			if (productType.equals("WV_OCN__2S"))
//				{
//				//WV L2:
//				//derive larger footprint from multiple smaller patches 
//					return WVFootPrintExtension.getBoundingPolygon(manifest.getAbsolutePath());
//				} else {
//					//should be 4 coordinates --> else exception
//					//copy from manifest -->  manifest is counterclockwise
//				}
//
//		} else {
//			//AUX and other
//			//???
//		}
		
		
		
		if (productType.equals("WV_OCN__2S")||
			productType.equals("WV_SLC__1S")||
			productType.equals("WV_GRDM_1S"))
		{
			return WVFootPrintExtension.getBoundingPolygon(manifest.getAbsolutePath());
		} else if(productType.equals("WV_RAW__0S")){
			
		} else if(productType.matches(".._RAW__0.")) {
			//copy from manifest -->  manifest is counterclockwise
		} else 
		
		
		
		try {
			// Extract all coordinates (seperated by **, last index is not a
			// coordinates)
			String[] coordinatesArray = rawCoordinates.split(";");
			int nbCoordinates = coordinatesArray.length;

			if (nbCoordinates <= 1) {
				// Only one coordinates

				String[] coordinatesTmp = coordinatesArray[0].split(" ");

				if (coordinatesTmp.length <= 2) { // BBOX type (envelope in ES)
					geoShape.put("type", "envelope");
					for (String coord : coordinatesTmp) {
						String[] tmp = coord.split(",");
						coordinates.put(new JSONArray("[" + tmp[1] + "," + tmp[0] + "]"));
					}
					if (PASS_ASC.equals(pass)) {
						geoShape.put("orientation", "counterclockwise");
					} else {
						geoShape.put("orientation", "clockwise");
					}
					geoShape.put("coordinates", coordinates);
				} else { // Polygon type
					geoShape.put("type", "polygon");
					for (String coord : coordinatesTmp) {
						String[] tmp = coord.split(",");
						coordinates.put(new JSONArray("[" + tmp[1] + "," + tmp[0] + "]"));
					}
					// If it is not a closed polygon
					if (!coordinatesTmp[0].equals(coordinatesTmp[coordinatesTmp.length - 1])) {
						String[] tmp = coordinatesTmp[0].split(",");
						coordinates.put(new JSONArray("[" + tmp[1] + "," + tmp[0] + "]"));
					}
					geoShape.put("orientation", "clockwise");
					geoShape.put("coordinates", new JSONArray().put(coordinates));
				}
			} else if (nbCoordinates == 2) {
				//WV L0 Products
				geoShape.put("type", "envelope");
				// Le premier point doit être extrait à partir de l’avant
				// dernier coordinate, et avec le premier point.
				String[] coordinatesTmp1 = coordinatesArray[1].split(" ");
				String[] tmp1 = coordinatesTmp1[1].split(",");
				coordinates.put(new JSONArray("[" + tmp1[1] + "," + tmp1[0] + "]"));
				// Le second point doit être extrait à partir du dernier
				// coordinate, et avec le second point
				String[] coordinatesTmp2 = coordinatesArray[0].split(" ");
				String[] tmp2 = coordinatesTmp2[3].split(",");
				coordinates.put(new JSONArray("[" + tmp2[1] + "," + tmp2[0] + "]"));
				if (PASS_ASC.equals(pass)) {
					geoShape.put("orientation", "counterclockwise");
				} else {
					geoShape.put("orientation", "clockwise");
				}
				geoShape.put("coordinates", coordinates);

			} else if (nbCoordinates == 3) {
				// Several coordinates
				geoShape.put("type", "polygon");
				// Le premier point = 1er coordonnée du 3eme point.
				String[] coordinatesTmp1 = coordinatesArray[2].split(" ");
				String[] tmp1 = coordinatesTmp1[0].split(",");
				coordinates.put(new JSONArray("[" + tmp1[1] + "," + tmp1[0] + "]"));
				// Le second point = 2eme coordonnée du 2eme point
				String[] coordinatesTmp2 = coordinatesArray[1].split(" ");
				String[] tmp2 = coordinatesTmp2[1].split(",");
				coordinates.put(new JSONArray("[" + tmp2[1] + "," + tmp2[0] + "]"));
				// Le troisième point = 3eme coordonnée du 2eme point
				String[] coordinatesTmp3 = coordinatesArray[1].split(" ");
				String[] tmp3 = coordinatesTmp3[2].split(",");
				coordinates.put(new JSONArray("[" + tmp3[1] + "," + tmp3[0] + "]"));
				// Le quatrième point = 4eme coordonnée du 1er point
				String[] coordinatesTmp4 = coordinatesArray[0].split(" ");
				String[] tmp4 = coordinatesTmp4[3].split(",");
				coordinates.put(new JSONArray("[" + tmp4[1] + "," + tmp4[0] + "]"));
				// On ferme le polygon
				coordinates.put(new JSONArray("[" + tmp1[1] + "," + tmp1[0] + "]"));
				geoShape.put("orientation", "counterclockwise");
				geoShape.put("coordinates", new JSONArray().put(coordinates));

			} else {
				// Several coordinates
				geoShape.put("type", "polygon");
				// Le premier point doit être extrait à partir de l’avant
				// dernier coordinate, et avec le premier point.
				String[] coordinatesTmp1 = coordinatesArray[nbCoordinates - 2].split(" ");
				String[] tmp1 = coordinatesTmp1[0].split(",");
				coordinates.put(new JSONArray("[" + tmp1[1] + "," + tmp1[0] + "]"));
				// Le second point doit être extrait à partir du dernier
				// coordinate, et avec le second point
				String[] coordinatesTmp2 = coordinatesArray[nbCoordinates - 1].split(" ");
				String[] tmp2 = coordinatesTmp2[1].split(",");
				coordinates.put(new JSONArray("[" + tmp2[1] + "," + tmp2[0] + "]"));
				// Le troisième point doit être extrait à partir du second
				// coordinate, et avec le troisième point.
				String[] coordinatesTmp3 = coordinatesArray[1].split(" ");
				String[] tmp3 = coordinatesTmp3[2].split(",");
				coordinates.put(new JSONArray("[" + tmp3[1] + "," + tmp3[0] + "]"));
				// Le quatrième point doit être extrait à partir du premier
				// coordinate, et avec le quatrième point.
				String[] coordinatesTmp4 = coordinatesArray[0].split(" ");
				String[] tmp4 = coordinatesTmp4[3].split(",");
				coordinates.put(new JSONArray("[" + tmp4[1] + "," + tmp4[0] + "]"));
				// On ferme le polygon
				coordinates.put(new JSONArray("[" + tmp1[1] + "," + tmp1[0] + "]"));
				geoShape.put("orientation", "counterclockwise");
				geoShape.put("coordinates", new JSONArray().put(coordinates));
			}
		} catch (JSONException e) {
			throw new MetadataExtractionException(e);
		}
		return geoShape;
	}

	/**
	 * Function which return the number of slices in a segment
	 * 
	 * @param startTimeLong
	 * @param stopTimeLong
	 * @param type
	 * @return an int which is the number of Slices
	 */
	private int totalNumberOfSlice(String startTime, String stopTime, String type) {		
		final Duration duration = Duration.between(
				DateUtils.parse(stopTime), 
				DateUtils.parse(startTime)
		);

		float sliceLength = this.typeSliceLength.get(type);

		// Case of their is no slice information in manifest
		if (sliceLength <= 0) {
			return 1;
		}
		float overlap = this.typeOverlap.get(type);

		float tmpNumberOfSlices = (duration.get(ChronoUnit.SECONDS) - overlap) / sliceLength;
		double fracNumberOfSlices = tmpNumberOfSlices - Math.floor(tmpNumberOfSlices);
		int totalNumberOfSlices = 0;
		if ((fracNumberOfSlices * sliceLength) < overlap) {
			totalNumberOfSlices = (int) Math.floor(tmpNumberOfSlices);
		} else {
			totalNumberOfSlices = (int) Math.ceil(tmpNumberOfSlices);
		}
		return Math.max(totalNumberOfSlices, 1);
	}

	/**
	 * Function which extracts metadata from MPL EOF file
	 * 
	 * @param descriptor The file descriptor of the auxiliary file
	 * @param file       The file containing the metadata
	 * @return the json object with extracted metadata
	 * @throws MetadataExtractionException
	 * @throws MetadataMalformedException 
	 */
	public JSONObject processEOFFile(ConfigFileDescriptor descriptor, File file) throws MetadataExtractionException, MetadataMalformedException {
		try {
			// XSLT Transformation
			String xsltFilename = this.xsltDirectory + XSLT_MPL_EOF;
			Source xsltMPLEOF = new StreamSource(new File(xsltFilename));
			Transformer transformerMPL = transFactory.newTransformer(xsltMPLEOF);
			Source mplMetadataFile = new StreamSource(file);
			transformerMPL.transform(mplMetadataFile, new StreamResult(new File("tmp/output.xml")));
			// JSON creation
			JSONObject metadataJSONObject = XML.toJSONObject(readFile("tmp/output.xml", Charset.defaultCharset()));
			
			try {
				metadataJSONObject.put("validityStopTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("validityStopTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("validityStopTime");
			}
			
			try {
				metadataJSONObject.put("creationTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("creationTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("creationTime");
			}
			
			try {
				metadataJSONObject.put("validityStartTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("validityStartTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("validityStartTime");
			}
			metadataJSONObject.put("productName", descriptor.getProductName());
			metadataJSONObject.put("productClass", descriptor.getProductClass());
			metadataJSONObject.put("productType", descriptor.getProductType());
			metadataJSONObject.put("missionId", descriptor.getMissionId());
			metadataJSONObject.put("satelliteId", descriptor.getSatelliteId());
			metadataJSONObject.put("url", descriptor.getKeyObjectStorage());
			metadataJSONObject.put("insertionTime", DateUtils.formatToMetadataDateTimeFormat(LocalDateTime.now()));
			metadataJSONObject.put("productFamily", descriptor.getProductFamily().name());
			return metadataJSONObject;
		} catch (IOException | TransformerException | JSONException e) {
			throw new MetadataExtractionException(e);
		}
	}

	/**
	 * Function which extracts metadata from AUX EOF file
	 * 
	 * @param descriptor The file descriptor of the auxiliary file
	 * @param file       The file containing the metadata
	 * @return the json object with extracted metadata
	 * @throws MetadataExtractionException
	 * @throws MetadataMalformedException 
	 */
	public JSONObject processEOFFileWithoutNamespace(ConfigFileDescriptor descriptor, File file)
			throws MetadataExtractionException, MetadataMalformedException {
		try {
			// XSLT Transformation
			String xsltFilename = this.xsltDirectory + XSLT_AUX_EOF;
			Source xsltAUXEOF = new StreamSource(new File(xsltFilename));
			Transformer transformerAUX = transFactory.newTransformer(xsltAUXEOF);
			Source auxMetadataFile = new StreamSource(file);
			transformerAUX.transform(auxMetadataFile, new StreamResult(new File("tmp/output.xml")));
			// JSON creation
			JSONObject metadataJSONObject = XML.toJSONObject(readFile("tmp/output.xml", Charset.defaultCharset()));
			
			try {
				metadataJSONObject.put("validityStopTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("validityStopTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("validityStopTime");
			}
			
			try {
				metadataJSONObject.put("creationTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("creationTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("creationTime");
			}
			
			try {
				metadataJSONObject.put("validityStartTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("validityStartTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("validityStartTime");
			}
						
			metadataJSONObject.put("productName", descriptor.getProductName());
			metadataJSONObject.put("productClass", descriptor.getProductClass());
			metadataJSONObject.put("productType", descriptor.getProductType());
			metadataJSONObject.put("missionId", descriptor.getMissionId());
			metadataJSONObject.put("satelliteId", descriptor.getSatelliteId());
			metadataJSONObject.put("url", descriptor.getKeyObjectStorage());
			metadataJSONObject.put("insertionTime", DateUtils.formatToMetadataDateTimeFormat(LocalDateTime.now()));
			metadataJSONObject.put("productFamily", descriptor.getProductFamily().name());
			return metadataJSONObject;
		} catch (IOException | TransformerException | JSONException e) {
			throw new MetadataExtractionException(e);
		}
	}

	/**
	 * Function which extracts metadata from AUX XML file
	 * 
	 * @param descriptor The file descriptor of the auxiliary file
	 * @param file       The file containing the metadata
	 * @return the json object with extracted metadata
	 * @throws MetadataExtractionException
	 * @throws MetadataMalformedException 
	 */
	public JSONObject processXMLFile(ConfigFileDescriptor descriptor, File file) throws MetadataExtractionException, MetadataMalformedException {
		try {
			// XSLT Transformation
			String xsltFilename = this.xsltDirectory + XSLT_AUX_XML;
			Source xsltAUXXML = new StreamSource(new File(xsltFilename));
			Transformer transformerAUX = transFactory.newTransformer(xsltAUXXML);
			Source auxMetadataFile = new StreamSource(file);
			transformerAUX.transform(auxMetadataFile, new StreamResult(new File("tmp/output.xml")));
			// JSON creation
			JSONObject metadataJSONObject = XML.toJSONObject(readFile("tmp/output.xml", Charset.defaultCharset()));
			
			try {
				metadataJSONObject.put("validityStopTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("validityStopTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("validityStopTime");
			}
			
			try {
				metadataJSONObject.put("validityStartTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("validityStartTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("validityStartTime");
			}
			
			try {
				metadataJSONObject.put("creationTime",
						DateUtils.convertToMetadataDateTimeFormat(metadataJSONObject.getString("creationTime")));
			} catch(DateTimeParseException e) {
				throw new MetadataMalformedException("creationTime");
			}
			
			metadataJSONObject.put("productName", descriptor.getProductName());
			metadataJSONObject.put("productClass", descriptor.getProductClass());
			metadataJSONObject.put("productType", descriptor.getProductType());
			metadataJSONObject.put("missionId", descriptor.getMissionId());
			metadataJSONObject.put("satelliteId", descriptor.getSatelliteId());
			metadataJSONObject.put("url", descriptor.getKeyObjectStorage());
			metadataJSONObject.put("insertionTime", DateUtils.formatToMetadataDateTimeFormat(LocalDateTime.now()));
			metadataJSONObject.put("productFamily", descriptor.getProductFamily().name());
			return metadataJSONObject;

		} catch (IOException | TransformerException | JSONException e) {
			throw new MetadataExtractionException(e);
		}
	}

	/**
	 * Function which extracts metadata from AUX MANIFEST file
	 * 
	 * @param descriptor The file descriptor of the auxiliary file
	 * @param file       The file containing the metadata
	 * @return the json object with extracted metadata
	 * @throws MetadataExtractionException
	 * @throws MetadataMalformedException 
	 */
	public JSONObject processSAFEFile(ConfigFileDescriptor descriptor, File file) throws MetadataExtractionException, MetadataMalformedException {
		try {
			// XSLT Transformation
			String xsltFilename = this.xsltDirectory + XSLT_AUX_MANIFEST;
			Source xsltAUXMANIFEST = new StreamSource(new File(xsltFilename));
			Transformer transformerAUX = transFactory.newTransformer(xsltAUXMANIFEST);
			Source auxMetadataFile = new StreamSource(file);
			transformerAUX.transform(auxMetadataFile, new StreamResult(new File("tmp/output.xml")));
			// JSON creation
			JSONObject metadataJSONObject = XML.toJSONObject(readFile("tmp/output.xml", Charset.defaultCharset()));
			metadataJSONObject.put("validityStopTime", "9999-12-31T23:59:59.999999Z");
			metadataJSONObject.put("productName", descriptor.getProductName());
			metadataJSONObject.put("productType", descriptor.getProductType());
			metadataJSONObject.put("missionId", descriptor.getMissionId());
			metadataJSONObject.put("satelliteId", descriptor.getSatelliteId());
			metadataJSONObject.put("url", descriptor.getKeyObjectStorage());
			metadataJSONObject.put("insertionTime", DateUtils.formatToMetadataDateTimeFormat(LocalDateTime.now()));
			metadataJSONObject.put("productFamily", descriptor.getProductFamily().name());
			
			if (metadataJSONObject.has("validityStartTime")) {
				try {
					metadataJSONObject.put("validityStartTime",
						DateUtils.convertToMetadataDateTimeFormat((String)metadataJSONObject.get("validityStartTime")));
				} catch(DateTimeParseException e) {
					throw new MetadataMalformedException("validityStartTime");
				}
			}
					
			if (metadataJSONObject.has("validityStopTime")) {
				try {
					metadataJSONObject.put("validityStopTime",
						DateUtils.convertToMetadataDateTimeFormat((String)metadataJSONObject.get("validityStopTime")));
				} catch(DateTimeParseException e) {
					throw new MetadataMalformedException("validityStopTime");
				}
			}

			return metadataJSONObject;
		} catch (IOException | TransformerException | JSONException e) {
			throw new MetadataExtractionException(e);
		}
	}

	/**
	 * Function which extracts metadata from RAW file
	 * 
	 * @param descriptor The file descriptor of the raw file
	 * @return the json object with extracted metadata
	 * @throws MetadataExtractionException
	 */
	public JSONObject processRAWFile(EdrsSessionFileDescriptor descriptor) throws MetadataExtractionException {
		try {
			JSONObject metadataJSONObject = new JSONObject();
			metadataJSONObject.put("productName", descriptor.getProductName());
			metadataJSONObject.put("productType", descriptor.getEdrsSessionFileType().name());
			metadataJSONObject.put("sessionId", descriptor.getSessionIdentifier());
			metadataJSONObject.put("missionId", descriptor.getMissionId());
			metadataJSONObject.put("satelliteId", descriptor.getSatelliteId());
			metadataJSONObject.put("url", descriptor.getKeyObjectStorage());
			metadataJSONObject.put("insertionTime", DateUtils.formatToMetadataDateTimeFormat(LocalDateTime.now()));
			metadataJSONObject.put("productFamily", descriptor.getProductFamily().name());
			return metadataJSONObject;
		} catch (JSONException e) {
			throw new MetadataExtractionException(e);
		}
	}

	/**
	 * Function which extracts metadata from SESSION file
	 * 
	 * @param descriptor The file descriptor of the session file
	 * @return the json object with extracted metadata
	 * @throws MetadataExtractionException
	 */
	public JSONObject processSESSIONFile(EdrsSessionFileDescriptor descriptor) throws MetadataExtractionException {
		try {
			JSONObject metadataJSONObject = new JSONObject();
			metadataJSONObject.put("productName", descriptor.getProductName());
			metadataJSONObject.put("productType", descriptor.getEdrsSessionFileType().name());
			metadataJSONObject.put("sessionId", descriptor.getSessionIdentifier());
			metadataJSONObject.put("missionId", descriptor.getMissionId());
			metadataJSONObject.put("satelliteId", descriptor.getSatelliteId());
			metadataJSONObject.put("url", descriptor.getKeyObjectStorage());
			metadataJSONObject.put("insertionTime", DateUtils.formatToMetadataDateTimeFormat(LocalDateTime.now()));
			metadataJSONObject.put("productFamily", descriptor.getProductFamily().name());
			return metadataJSONObject;
		} catch (JSONException e) {
			throw new MetadataExtractionException(e);
		}
	}
	

	public JSONObject processL0Segment(OutputFileDescriptor descriptor, File manifestFile)
			throws MetadataExtractionException, MetadataMalformedException {
		try {
			// XSLT Transformation
			String xsltFilename = this.xsltDirectory + XSLT_L0_SEGMENT_MANIFEST;
			Source xsltL1MANIFEST = new StreamSource(new File(xsltFilename));
			Transformer transformerL0 = transFactory.newTransformer(xsltL1MANIFEST);
			Source l1File = new StreamSource(manifestFile);
			transformerL0.transform(l1File, new StreamResult(new File(OUTPUT_L0_SEGMENT_XML)));
			// JSON creation
			JSONObject metadataJSONObject = XML.toJSONObject(readFile(OUTPUT_L0_SEGMENT_XML, Charset.defaultCharset()));
			if (metadataJSONObject.has("startTime")) {
				try {
					String t = DateUtils.convertToMetadataDateTimeFormat((String)metadataJSONObject.getString("startTime")); 
					metadataJSONObject.put("startTime", t);
					metadataJSONObject.put("validityStartTime", t);
				} catch(DateTimeParseException e) {
					throw new MetadataMalformedException("validityStartTime");
				}
			}
			if (metadataJSONObject.has("stopTime")) {
				try {
					String t = DateUtils.convertToMetadataDateTimeFormat((String)metadataJSONObject.getString("stopTime")); 
					metadataJSONObject.put("stopTime", t);
					metadataJSONObject.put("validityStopTime", t);
				} catch(DateTimeParseException e) {
					throw new MetadataMalformedException("validityStopTime");
				}
			}
			String pass = PASS_DFT;
			if (metadataJSONObject.has("sliceCoordinates") && !metadataJSONObject.getString("pass").isEmpty()) {
				pass = metadataJSONObject.getString("pass");
			}
			if (metadataJSONObject.has("segmentCoordinates")) {
				metadataJSONObject.put("segmentCoordinates", processCoordinates(manifestFile,descriptor,
						metadataJSONObject.getString("segmentCoordinates"), pass));
			}
			metadataJSONObject.put("productName", descriptor.getProductName());
			metadataJSONObject.put("productClass", descriptor.getProductClass());
			metadataJSONObject.put("productType", descriptor.getProductType());
			metadataJSONObject.put("resolution", descriptor.getResolution());
			metadataJSONObject.put("missionId", descriptor.getMissionId());
			metadataJSONObject.put("satelliteId", descriptor.getSatelliteId());
			metadataJSONObject.put("swathtype", descriptor.getSwathtype());
			metadataJSONObject.put("polarisation", descriptor.getPolarisation());
			metadataJSONObject.put("dataTakeId", descriptor.getDataTakeId());
			metadataJSONObject.put("url", descriptor.getKeyObjectStorage());
			metadataJSONObject.put("processMode", descriptor.getMode());
			String dt = DateUtils.formatToMetadataDateTimeFormat(LocalDateTime.now());
			metadataJSONObject.put("insertionTime", dt);
			metadataJSONObject.put("creationTime", dt);
			metadataJSONObject.put("productFamily", descriptor.getProductFamily().name());
			return metadataJSONObject;
		} catch (IOException | TransformerException | JSONException e) {
			throw new MetadataExtractionException(e);
		}
	}
	/**
	 * Function which extracts metadata from product
	 * 
	 * @param descriptor
	 * @param manifestFile
	 * @param output
	 * 
	 * @return the json object with extracted metadata
	 * @throws MetadataExtractionException
	 * @throws MetadataMalformedException 
	 */
	public JSONObject processProduct(OutputFileDescriptor descriptor, ProductFamily productFamily, File manifestFile)
			throws MetadataExtractionException, MetadataMalformedException {
		try {
			
	
			
			// XSLT Transformation
			Source xsltMANIFEST = new StreamSource(new File(this.xsltDirectory + xsltMap.get(productFamily)));
			Transformer transformer = transFactory.newTransformer(xsltMANIFEST);
			Source inputFile = new StreamSource(manifestFile);
			transformer.transform(inputFile, new StreamResult(new File(OUTPUT_XML)));
			// JSON creation
			JSONObject metadataJSONObject = XML.toJSONObject(readFile(OUTPUT_XML, Charset.defaultCharset()));
			String pass = PASS_DFT;
			if (metadataJSONObject.has("sliceCoordinates") && !metadataJSONObject.getString("pass").isEmpty()) {
				pass = metadataJSONObject.getString("pass");
			}
			if (metadataJSONObject.has("sliceCoordinates")
					&& !metadataJSONObject.getString("sliceCoordinates").isEmpty()) {
				metadataJSONObject.put("sliceCoordinates", processCoordinates(manifestFile,descriptor,
						metadataJSONObject.getString("sliceCoordinates"), pass));
			}

			
			if (metadataJSONObject.has("startTime")) {
				try {
					String t = DateUtils.convertToMetadataDateTimeFormat((String)metadataJSONObject.getString("startTime")); 
					metadataJSONObject.put("startTime", t);
					metadataJSONObject.put("validityStartTime", t);
				} catch(DateTimeParseException e) {
					throw new MetadataMalformedException("validityStartTime");
				}
			}
			
			if (metadataJSONObject.has("stopTime")) {
				try {
					String t = DateUtils.convertToMetadataDateTimeFormat((String)metadataJSONObject.getString("stopTime")); 
					metadataJSONObject.put("stopTime", t);
					metadataJSONObject.put("validityStopTime", t);
				} catch(DateTimeParseException e) {
					throw new MetadataMalformedException("validityStopTime");
				}
			}
			
			if(ProductFamily.L0_ACN.equals(productFamily)|| ProductFamily.L0_SLICE.equals(productFamily)) {
				
				if (!metadataJSONObject.has("sliceNumber")) {
					metadataJSONObject.put("sliceNumber", 1);
				} else if (StringUtils.isEmpty(metadataJSONObject.get("sliceNumber").toString())) {
					metadataJSONObject.put("sliceNumber", 1);
				}	
				if (Arrays.asList("A","C","N").contains(descriptor.getProductClass())) {
					if (metadataJSONObject.has("startTime") && metadataJSONObject.has("stopTime")) {
						metadataJSONObject.put("totalNumberOfSlice",
								totalNumberOfSlice(
										metadataJSONObject.getString("startTime"),
										metadataJSONObject.getString("stopTime"),
										descriptor.getSwathtype().matches("S[1-6]") ? "SM" : descriptor.getSwathtype()
								)
						);
					}
				}
			}

			metadataJSONObject.put("productName", descriptor.getProductName());
			metadataJSONObject.put("productClass", descriptor.getProductClass());
			metadataJSONObject.put("productType", descriptor.getProductType());
			metadataJSONObject.put("resolution", descriptor.getResolution());
			metadataJSONObject.put("missionId", descriptor.getMissionId());
			metadataJSONObject.put("satelliteId", descriptor.getSatelliteId());
			metadataJSONObject.put("swathtype", descriptor.getSwathtype());
			metadataJSONObject.put("polarisation", descriptor.getPolarisation());
			metadataJSONObject.put("dataTakeId", descriptor.getDataTakeId());
			metadataJSONObject.put("url", descriptor.getKeyObjectStorage());
			String dt = DateUtils.formatToMetadataDateTimeFormat(LocalDateTime.now());
			metadataJSONObject.put("insertionTime", dt);
			metadataJSONObject.put("creationTime", dt);
			metadataJSONObject.put("productFamily", descriptor.getProductFamily().name());
			metadataJSONObject.put("processMode", descriptor.getMode());
			return metadataJSONObject;
		} catch (IOException | TransformerException | JSONException e) {
			throw new MetadataExtractionException(e);
		}
	}



}
