package com.inbravo.cad.rest.service.crm.ms.auth;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

import com.inbravo.cad.exception.CADException;
import com.inbravo.cad.exception.CADResponseCodes;
import com.inbravo.cad.internal.service.dto.CADUser;
import com.inbravo.cad.internal.service.dto.BasicObject;
import com.inbravo.cad.internal.service.dto.Tenant;
import com.inbravo.cad.rest.service.crm.ms.dto.MSCRMUserInformation;
import com.microsoft.schemas.crm._2007.CrmDiscoveryService.CrmDiscoveryServiceLocator;
import com.microsoft.schemas.crm._2007.CrmDiscoveryService.CrmDiscoveryServiceSoap;
import com.microsoft.schemas.crm._2007.CrmDiscoveryService.OrganizationDetail;
import com.microsoft.schemas.crm._2007.CrmDiscoveryService.RetrieveCrmTicketRequest;
import com.microsoft.schemas.crm._2007.CrmDiscoveryService.RetrieveCrmTicketResponse;
import com.microsoft.schemas.crm._2007.CrmDiscoveryService.RetrieveOrganizationsRequest;
import com.microsoft.schemas.crm._2007.CrmDiscoveryService.RetrieveOrganizationsResponse;

/**
 * 
 * @author amit.dixit
 * 
 */
public final class MSCRMDiscoveryManager {

  private final Logger logger = Logger.getLogger(MSCRMDiscoveryManager.class.getName());

  private MSAuthManager mSAuthManager;

  private String crmDiscoveryServiceEndpoint = SOAPExecutor.CRM_DISCOVERY_SERVICE_ENDPOINT;

  /**
   * This API will be used for finding the organization information of an agent/tenant
   * 
   * @param userId
   * @param password
   * @param passportTicket
   * @return
   * @throws ServiceException
   * @throws JaxenException
   * @throws IOException
   * @throws SOAPException
   */
  private final OrganizationDetail retrieveOrganizationDetail(final String userId, final String password, final String passportTicket,
      final CrmDiscoveryServiceSoap crmDiscoveryService) throws IOException {

    final RetrieveOrganizationsRequest request = new RetrieveOrganizationsRequest();
    request.setPassportTicket(passportTicket);
    request.setUserId(userId);
    request.setPassword(password);

    /* Get organization information from discovery service */
    final RetrieveOrganizationsResponse response = (RetrieveOrganizationsResponse) crmDiscoveryService.execute(request);
    final OrganizationDetail[] orgDetails = response.getOrganizationDetails();

    /* Return orgnization information */
    if (orgDetails != null && orgDetails.length != 0) {
      return orgDetails[0];
    } else {
      return null;
    }
  }

  /**
   * This API will be used for generating a CRM ticket for communicating with Microsoft CRM
   * 
   * @param userId
   * @param password
   * @return
   * @throws ServiceException
   * @throws JaxenException
   * @throws IOException
   * @throws SOAPException
   */
  public final MSCRMUserInformation getMSCRMUserInformation(final BasicObject basicObject) throws Exception {

    logger.debug("---Inside getMSCRMUserInformation");
    try {

      String userName = null;
      String password = null;
      String crmServiceURL = null;
      String crmServiceProtocal = null;

      /* Check if agent/tenant */
      if (basicObject instanceof CADUser) {

        /* Type cast it to agent */
        final CADUser agent = (CADUser) basicObject;

        logger.debug("---Inside getMSCRMUserInformation for agent: " + agent.getName());

        /* get CRM credentials */
        userName = agent.getCrmUserid();
        password = agent.getCrmPassword();
        crmServiceURL = agent.getCrmServiceURL();
        crmServiceProtocal = agent.getCrmServiceProtocol();

      } else /* Check if agent/tenant */
      if (basicObject instanceof Tenant) {

        /* Type cast it to tenant */
        final Tenant tenant = (Tenant) basicObject;

        logger.debug("---Inside getMSCRMUserInformation for tenant: " + tenant.getName());

        /* get CRM credentials */
        userName = tenant.getCrmUserid();
        password = tenant.getCrmPassword();
        crmServiceURL = tenant.getCrmServiceURL();
        crmServiceProtocal = tenant.getCrmServiceProtocol();
      }

      /* First get Microsoft passport ticket */
      final String[] passportTicket = mSAuthManager.getCRMAuthToken(basicObject);

      /* Create discovery service locator */
      final CrmDiscoveryServiceLocator locator = new CrmDiscoveryServiceLocator();

      /* Create discovery service */
      final CrmDiscoveryServiceSoap crmDiscoveryService =
          locator.getCrmDiscoveryServiceSoap12(new URL(crmServiceProtocal + "://" + crmServiceURL + crmDiscoveryServiceEndpoint));

      /* First get organization details */
      final OrganizationDetail org = this.retrieveOrganizationDetail(userName, password, passportTicket[0], crmDiscoveryService);

      /* Create new local DTO */
      MSCRMUserInformation mSCRMUserInformation = new MSCRMUserInformation();

      /* Set organization information */
      if (org != null && org.getOrganizationName() != null) {

        /* Create request object to get CRM ticket */
        final RetrieveCrmTicketRequest request = new RetrieveCrmTicketRequest();
        request.setPassportTicket(passportTicket[0]);
        request.setOrganizationName(org.getOrganizationName());

        /* Get CRM ticket from Discovery service */
        final RetrieveCrmTicketResponse response = (RetrieveCrmTicketResponse) crmDiscoveryService.execute(request);

        /* Add the necessary information in local DTO */
        mSCRMUserInformation.setCrmTicket(response.getCrmTicket());
        mSCRMUserInformation.setOrganizationName(org.getOrganizationName());
        mSCRMUserInformation.setPassportTicket(passportTicket[0]);
        mSCRMUserInformation.setCrmServiceURL(org.getCrmServiceUrl());
      } else {

        /* This check is to disallocate the object from memory */
        mSCRMUserInformation = null;
      }
      return mSCRMUserInformation;
    } catch (final MalformedURLException e) {
      throw new CADException(CADResponseCodes._1009 + "Protocol type is not found from the agent/tenant configuration", e);
    } catch (final AxisFault e) {
      throw new CADException(CADResponseCodes._1013 + "Recieved a web service error", e);
    } catch (final RemoteException e) {
      throw new CADException(CADResponseCodes._1013 + "Communication error", e);
    }
  }

  public final String getCrmDiscoveryServiceEndpoint() {
    return crmDiscoveryServiceEndpoint;
  }

  public final void setCrmDiscoveryServiceEndpoint(final String crmDiscoveryServiceEndpoint) {
    this.crmDiscoveryServiceEndpoint = crmDiscoveryServiceEndpoint;
  }

  /**
   * @return the mSAuthManager
   */
  public final MSAuthManager getmSAuthManager() {
    return this.mSAuthManager;
  }

  /**
   * @param mSAuthManager the mSAuthManager to set
   */
  public final void setmSAuthManager(final MSAuthManager mSAuthManager) {
    this.mSAuthManager = mSAuthManager;
  }
}