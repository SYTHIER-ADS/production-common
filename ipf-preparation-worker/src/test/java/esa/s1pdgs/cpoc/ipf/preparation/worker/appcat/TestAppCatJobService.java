package esa.s1pdgs.cpoc.ipf.preparation.worker.appcat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;

public class TestAppCatJobService {
	@Mock
	private AppCatalogJobClient appCatClient;
	
	@Mock
    private GracePeriodHandler gracePeriodHandler;
	
	private AppCatJobService uut;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        uut = new AppCatJobService(appCatClient, gracePeriodHandler);
    }
    
	@Test
	public final void testCreate() throws Exception {
		final AppDataJob job = new AppDataJob();		
    	doReturn(job).when(appCatClient).newJob(job);
		uut.create(job);
		verify(appCatClient, times(1)).newJob(Mockito.eq(job));		
	}
}
