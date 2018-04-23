package fr.viveris.s1pdgs.jobgenerator.service;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.viveris.s1pdgs.jobgenerator.exception.EdrsSessionException;
import fr.viveris.s1pdgs.jobgenerator.exception.ObjectStorageException;
import fr.viveris.s1pdgs.jobgenerator.model.EdrsSessionFile;
import fr.viveris.s1pdgs.jobgenerator.service.s3.SessionFilesS3Services;

/**
 * Class for managing EDRS session files
 * 
 * @author Cyrielle Gailliard
 *
 */
@Service
public class EdrsSessionFileService {

	/**
	 * S3 service
	 */
	private final SessionFilesS3Services s3Services;

	/**
	 * XML converter
	 */
	private final XmlConverter xmlConverter;

	/**
	 * Local directory to upload EDRS session file
	 */
	private final String pathTempDirectory;

	/**
	 * Constructor
	 * 
	 * @param s3Services
	 * @param xmlConverter
	 * @param pathTempDirectory
	 */
	@Autowired
	public EdrsSessionFileService(final SessionFilesS3Services s3Services, final XmlConverter xmlConverter,
			@Value("${level0.dir-extractor-sessions}") final String pathTempDirectory) {
		this.s3Services = s3Services;
		this.xmlConverter = xmlConverter;
		this.pathTempDirectory = pathTempDirectory;
	}

	/**
	 * Create an object EdrsSessionFile from the key in object storage. We will get
	 * the file from the object storage and convert it into an object
	 * 
	 * @param keyObjectStorage
	 * @param channelId
	 * @return
	 * @throws EdrsSessionException
	 * @throws ObjectStorageException
	 */
	public EdrsSessionFile createSessionFile(String keyObjectStorage, int channelId)
			throws EdrsSessionException, ObjectStorageException {
		// Check
		if (channelId != 1 && channelId != 2) {
			throw new EdrsSessionException(String.format("[key %s] Invalid channel %d", keyObjectStorage, channelId));
		}

		// Extract filename from the key object storage
		String id = keyObjectStorage;
		int lastIndex = keyObjectStorage.lastIndexOf('/');
		if (lastIndex != -1 && lastIndex < keyObjectStorage.length() - 1) {
			id = keyObjectStorage.substring(lastIndex + 1);
		}

		File tmpFile = null;
		try {
			// Download file
			tmpFile = s3Services.getFile(keyObjectStorage, this.pathTempDirectory + id);

			return (EdrsSessionFile) xmlConverter.convertFromXMLToObject(tmpFile.getAbsolutePath());

		} catch (IOException | JAXBException | IllegalArgumentException e) {
			throw new EdrsSessionException(String.format("[key %s] %s", keyObjectStorage, channelId));
		} finally {
			if (tmpFile != null) {
				tmpFile.delete();
			}
		}
	}

}
