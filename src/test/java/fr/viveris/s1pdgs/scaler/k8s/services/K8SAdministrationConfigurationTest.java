package fr.viveris.s1pdgs.scaler.k8s.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import fr.viveris.s1pdgs.scaler.k8s.K8SProperties;
import io.fabric8.kubernetes.client.Config;

public class K8SAdministrationConfigurationTest {

    private static String KEY =
            "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFcFFJQkFBS0NBUUVBcmJsSVpvYTdrQVpXM3pYb3h5K1hGZzVOb0hUM1lNRjJvSnBJWkRRT25hU0tPUkd0CmFLMzhVWmlpQWRVcXBtS0RxZG1ZcVZiOGJyNXR4MzZXWS85Q2hhUWRnWFlSNEJWeXcrMXBTOW93Y1FQK0ZXTEEKQ2p6UmNBQ0lmeGx0SGdNaHRCZm1IbUh2dGcyWnkwUGFwN05SRHRrTWg4YW5vcWwwNzFMa0Y2b0JxM2gxY1pGMAplWEZMaDkwb05rOWhrYUIvTEFHVzk3RHpNeXpUcUJRVy9XdDNQTjVvVkNHOUo2RnMyaXcvNTNUcWRoVnVTVjFwCjlDRUZHSGdhN2l1UzNILzlISWk1dGZ0WFdlckV4VGJxV1J1WFVxTDB6UWtyV1hzTUkyME9yTHJiOU5ndng2eGUKZVhkanpLZHlxb1Z2SmNDN1pQUjZQNHZWT25BZUFkWitIVE1pU1FJREFRQUJBb0lCQVFDTlVobU1sZlFFc0xPQQp0dmthK0NMZkpWbU91emYyTk10TTBOVXM5cEFoTzVYWjRRQ0JGSEFhN0tCMS96UEgwSUlzN0w5Y21rK1Z3MEhJCnRMaWd0aWttVUNCVWpYanpJbCtPOVJYZ1I2bDZkblgrYmF1dGFGWXoxNnN5UWJ2YlcwN1NrMUoyeXRMVzlXOXoKeEVvZWZDZm5mVGZOU0JSY3BaOWpoTG5hYWtrYmMvV2haam5Xbk8vbHBRQ3dBQ2xzaU9rQVNEOExoa1AwNUt3dwpqRHpzS0hMb3gwNitUT212NHE0YXRTV0RTM2RUTUgzNXY2eXFXUnhFdWo0SGJDRm5sNXlXM0kyTGNFc3RiR01WClFXTnhvNm9HZEpFcTZOV2g4NTg3RzBWYW9ZY1MwT1JxcmJuS04zTm4vdHdibVh1b3MzZjUwODJKeTU1NWhtVFEKRXlkT1djbUJBb0dCQU9IdkZTM2YvSE9ONlM5ZlUvMVpWWWUwbnFlSzZrLzNPK2Q4SW11QlFjand5R0s2aklKVwo0SFhVTmdHdnFXQzI4T21XQXlEUCtJL2l4Tk5LeWpvOXU3MW1DSW9FQXNSeVVydTRyZzRjSEVzQ2F3ZVJGclY0Ckc0dXViRTNhamlJQ3Bpa3lIbVZidTFFUDJQeDcyNC9FZlBVUGV0UnVsaFp1ZXl6L3lJelpkNGJSQW9HQkFNVFgKalBKVC9Vby9YNkhkUEJXaWRIVHJCZUVHZXlWUFo1cXNob0FXeG5JbC9MemIzN1RKcDZlUGpDdG0rbHVWWUpDQQptVmllRUMxRS94b1c4ekF1TlBFeUphM3dyd1Z3ZFMrZnNIS3kzZ1hrMk1CZEV5NnVDazZNVVhjSUg3U0lZeUlICldqeG1VV2IvZHRlWEN2MEZVRm92aGNlNmpJNGEvdDE5dXdjRHN6SDVBb0dBSXovN0RQSkNZQUVISGJZQTA2bEoKZCtmTlRSU1daQzJOc2hzaS82VG1ENlRKanVYT0lGUFBwM0taam4vS3JHVStoeU01ajdnQzd1Z1JqMm0rellGdQpOaW1pTVc1WXhDK1dDdVhRZWpFV2xQbG1tNEtlaVdlWTNKMDFGcHgveW55aFVoSVl2ZldtN3duSzcvR2ZHdm9zCkNNd0dmUGhZQUYzeVo5M3NlMVUrbWRFQ2dZRUFrVTkrWVRYWGVnUW1tTnMxQzlPTm5QSVN1UGVMMlJNeExHSEkKT0s2WGVKVEthckQyQ0FRRm5CREFMUm9zSDRlNmJYSkJ3Y1dOczUySHBMN2tiK0RzZkZIRXR3OUNaUVdMdk1ocAovWUpGbkp3LzFtSGZVMHB2bVdURWp0YVVjVFZ0MlNVTVhDSThYWWloTnEzdUVyTGxpbTRpbURzQ243VVdDSFJVCnFPejJVQ0VDZ1lFQW1OV01lelg4QlRka1pXVGZOV1Y3YThVNjhQSllFSUoxVXVuSGFEb1pHWDJOOWU0dkJzSVIKMkJzUWZoTUp3UjB6Y2JUNXN5TnNWN0VGVVJlejZnNkxrVStRZ2xFV0tCWXg3VHJ1Nmw1c1k2Z1pLQmlpS1FUeAo1TFlRRk1jaEJiVjUyaFdRS0xzejlWOUdCWWhZeTZYN1oxTjA2eTByczFjcllpRGQwRm1yamJ3PQotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQo=";
    private static String CERT =
            "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUM4akNDQWRxZ0F3SUJBZ0lJWW9vM2pMZUhwZTh3RFFZSktvWklodmNOQVFFTEJRQXdGVEVUTUJFR0ExVUUKQXhNS2EzVmlaWEp1WlhSbGN6QWVGdzB4T0RBeU1qQXhOREk0TkRSYUZ3MHhPVEF5TWpBeE5ESTRORFphTURReApGekFWQmdOVkJBb1REbk41YzNSbGJUcHRZWE4wWlhKek1Sa3dGd1lEVlFRREV4QnJkV0psY201bGRHVnpMV0ZrCmJXbHVNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDQVFFQXJibElab2E3a0FaVzN6WG8KeHkrWEZnNU5vSFQzWU1GMm9KcElaRFFPbmFTS09SR3RhSzM4VVppaUFkVXFwbUtEcWRtWXFWYjhicjV0eDM2VwpZLzlDaGFRZGdYWVI0QlZ5dysxcFM5b3djUVArRldMQUNqelJjQUNJZnhsdEhnTWh0QmZtSG1IdnRnMlp5MFBhCnA3TlJEdGtNaDhhbm9xbDA3MUxrRjZvQnEzaDFjWkYwZVhGTGg5MG9Oazloa2FCL0xBR1c5N0R6TXl6VHFCUVcKL1d0M1BONW9WQ0c5SjZGczJpdy81M1RxZGhWdVNWMXA5Q0VGR0hnYTdpdVMzSC85SElpNXRmdFhXZXJFeFRicQpXUnVYVXFMMHpRa3JXWHNNSTIwT3JMcmI5Tmd2eDZ4ZWVYZGp6S2R5cW9WdkpjQzdaUFI2UDR2Vk9uQWVBZForCkhUTWlTUUlEQVFBQm95Y3dKVEFPQmdOVkhROEJBZjhFQkFNQ0JhQXdFd1lEVlIwbEJBd3dDZ1lJS3dZQkJRVUgKQXdJd0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFBWDZXN0lpRld1eFR4Y0dsemEvVkIxRVhaekY4ZEpXNTZRTgpQbzRVU2plRlcyNWluSi84c09uVkFxaEJvcTZKdm1UamVBN2FtcXNBRlF3WStDQXpsdFJtNFNyWmxUcUNVR2FsCkFlU2c0bHBvSGhPMmVLcll6bE1aWjFDbGRtbU1CdkplWlZUS0I0Y1dLTTkyQkdtOUVFRkJVUW5hUjlSVnhTZGcKTnFmSTlLcVRYRjRQbXFDbnR2R3Y2MkhOM3IvNkJjbWM3NXdPVHdGdGVBL3YxanhWa0xVcW5pSi9VQk11MFhzdgoyU0ZIVmJRd1VZOGdIWG1LTHZZdGtIWjd0QVpVQlVseW91SW84VVZ1UzZ4Smc4NUZuZHBYcjgxQ0ZQaitITDV3CkNvNXdub0ppY1BDY3hNM1IwYWdMTExxc2E0QzJhQk9GRkZ6aERncXJtdWRVWDdjYzAwQT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=";

    @Mock
    private K8SProperties properties;

    private K8SAdminConfiguration admin;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        doReturn("http://1.2.3.4:80/").when(properties).getMasterUrl();
        doReturn("username").when(properties).getUsername();
        doReturn(KEY).when(properties).getClientKey();
        doReturn(CERT).when(properties).getClientCertData();
        doReturn("space").when(properties).getNamespace();

        admin = new K8SAdminConfiguration(properties);
    }

    @Test
    public void testConfig() {
        Config config = admin.k8sConfig();
        config.getClientCertData();
        assertEquals("http://1.2.3.4:80/", config.getMasterUrl());
        assertEquals("username", config.getUsername());
        assertEquals("space", config.getNamespace());
        verify(properties, times(1)).getMasterUrl();
        verify(properties, times(1)).getUsername();
        verify(properties, times(1)).getClientKey();
        verify(properties, times(1)).getClientCertData();
        verify(properties, times(1)).getNamespace();
        verifyNoMoreInteractions(properties);
    }

    @Test
    public void testClient() {
        admin.k8sClient();
    }
}
