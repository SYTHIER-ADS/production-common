package fr.viveris.s1pdgs.level0.wrapper.services.s3;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;

@Service
public class L0SlicesS3Services extends AbstractS3Services {

	@Autowired
	public L0SlicesS3Services(AmazonS3 s3client, @Value("${storage.buckets.l0-slices}") String bucketName) {
		super(s3client, bucketName);
	}
}
