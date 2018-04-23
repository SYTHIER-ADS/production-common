package fr.viveris.s1pdgs.jobgenerator.utils;

import java.util.Arrays;

import fr.viveris.s1pdgs.jobgenerator.model.l1routing.L1Route;
import fr.viveris.s1pdgs.jobgenerator.model.l1routing.L1RouteFrom;
import fr.viveris.s1pdgs.jobgenerator.model.l1routing.L1RouteTo;
import fr.viveris.s1pdgs.jobgenerator.model.l1routing.L1Routing;

public class TestL1Utils {

	public static L1Routing buildL1Routing() {
		L1Routing r = new L1Routing();
		r.addRoute(new L1Route(new L1RouteFrom("EN", "A"),
				new L1RouteTo(Arrays.asList("EN_RAW__0_GRDF_1.xml",
						"EN_RAW__0_GRDH_1.xml", "EN_RAW__0_GRDM_1.xml",
						"EN_RAW__0_SLC__1.xml", "EN_RAW__0_SLC__1_GRDF_1.xml",
						"EN_RAW__0_SLC__1_GRDH_1.xml"))));
		r.addRoute(new L1Route(new L1RouteFrom("EN", "B"),
				new L1RouteTo(Arrays.asList("EN_RAW__0_SLC__1.xml",
						"EN_RAW__0_SLC__1_GRDF_1.xml", "EN_RAW__0_SLC__1_GRDH_1.xml"))));
		r.addRoute(new L1Route(new L1RouteFrom("EW", "A"),
				new L1RouteTo(Arrays.asList("EW_RAW__0_GRDH_1.xml",
						"EW_RAW__0_GRDM_1.xml", "EW_RAW__0_SLC__1.xml",
						"EW_RAW__0_SLC__1_GRDH_1.xml", "EW_RAW__0_SLC__1_GRDM_1.xml"))));
		r.addRoute(new L1Route(new L1RouteFrom("EW", "B"),
				new L1RouteTo(Arrays.asList("EW_RAW__0_SLC__1.xml",
						"EW_RAW__0_SLC__1_GRDH_1.xml", "EW_RAW__0_SLC__1_GRDM_1.xml"))));
		r.addRoute(new L1Route(new L1RouteFrom("IW", "A"),
				new L1RouteTo(Arrays.asList("IW_RAW__0_GRDH_1.xml",
						"IW_RAW__0_GRDM_1.xml", "IW_RAW__0_SLC__1.xml",
						"IW_RAW__0_SLC__1_GRDH_1.xml", "IW_RAW__0_SLC__1_GRDM_1.xml"))));
		r.addRoute(new L1Route(new L1RouteFrom("IW", "B"),
				new L1RouteTo(Arrays.asList("IW_RAW__0_SLC__1.xml",
						"IW_RAW__0_SLC__1_GRDH_1.xml", "IW_RAW__0_SLC__1_GRDM_1.xml"))));
		r.addRoute(new L1Route(new L1RouteFrom("SM", "A"),
				new L1RouteTo(Arrays.asList("SM_RAW__0_GRDF_1.xml",
						"SM_RAW__0_GRDH_1.xml", "SM_RAW__0_GRDM_1.xml",
						"SM_RAW__0_SLC__1.xml", "SM_RAW__0_SLC__1_GRDF_1.xml",
						"SM_RAW__0_SLC__1_GRDH_1.xml"))));
		r.addRoute(new L1Route(new L1RouteFrom("SM", "B"),
				new L1RouteTo(Arrays.asList("SM_RAW__0_SLC__1.xml",
						"SM_RAW__0_SLC__1_GRDF_1.xml", "SM_RAW__0_SLC__1_GRDH_1.xml"))));

		return r;
	}

}
