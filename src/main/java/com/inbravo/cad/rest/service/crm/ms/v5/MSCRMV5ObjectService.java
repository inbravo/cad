package com.inbravo.cad.rest.service.crm.ms.v5;

import org.apache.log4j.Logger;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.inbravo.cad.exception.CADException;
import com.inbravo.cad.exception.CADResponseCodes;
import com.inbravo.cad.rest.constants.CRMConstants.MSCRMObjectType;
import com.inbravo.cad.rest.resource.CADCommandObject;
import com.inbravo.cad.rest.service.crm.ms.MSCRMObjectService;

/**
 * 
 * @author amit.dixit
 * 
 */
public final class MSCRMV5ObjectService extends MSCRMObjectService implements ApplicationContextAware {

  private final Logger logger = Logger.getLogger(MSCRMV5ObjectService.class.getName());

  private ApplicationContext applicationContext;

  private MSCRMV5AccountService mSCRMV5AccountService;

  private MSCRMV5TaskService mSCRMV5TaskService;

  private MSCRMV5ContactService mSCRMV5ContactService;

  private MSCRMV5LeadService mSCRMV5LeadService;

  private MSCRMV5IncidentService mSCRMV5IncidentService;

  private MSCRMV5CaseService mSCRMV5CaseService;

  private MSCRMV5OpportunityService mSCRMV5OpportunityService;

  public MSCRMV5ObjectService() {
    super();
  }

  @Override
  public final CADCommandObject createObject(final CADCommandObject cADCommandObject) throws Exception {
    logger.debug("---Inside createObject");
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).createObject(cADCommandObject);
  }

  @Override
  public final boolean deleteObject(final CADCommandObject cADCommandObject, final String idToBeDeleted) throws Exception {
    logger.debug("---Inside deleteObject idToBeDeleted: " + idToBeDeleted);
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).deleteObject(cADCommandObject, idToBeDeleted);
  }

  @Override
  public final CADCommandObject getObjects(final CADCommandObject cADCommandObject) throws Exception {
    logger.debug("---Inside getObjects");
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).getObjects(cADCommandObject);
  }

  @Override
  public final CADCommandObject getObjects(final CADCommandObject cADCommandObject, final String query) throws Exception {
    logger.debug("---Inside getObjects query: " + query);
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).getObjects(cADCommandObject, query);
  }

  @Override
  public final CADCommandObject getObjects(final CADCommandObject cADCommandObject, final String query, final String select) throws Exception {
    logger.debug("---Inside getObjects query: " + query + " & select: " + select);
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).getObjects(cADCommandObject, query, select);
  }

  @Override
  public final CADCommandObject getObjects(final CADCommandObject cADCommandObject, final String query, final String select, final String order)
      throws Exception {
    logger.debug("---Inside getObjects query: " + query + " & select: " + select + " & order: " + order);
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).getObjects(cADCommandObject, query, select, order);
  }

  @Override
  public CADCommandObject getObjectsCount(CADCommandObject cADCommandObject) throws Exception {
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).getObjectsCount(cADCommandObject);
  }

  @Override
  public final CADCommandObject getObjectsCount(CADCommandObject cADCommandObject, final String query) throws Exception {
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).getObjectsCount(cADCommandObject, query);
  }

  @Override
  public final CADCommandObject updateObject(final CADCommandObject cADCommandObject) throws Exception {
    return this.getMSCRMObjectService(cADCommandObject.getObjectType()).updateObject(cADCommandObject);
  }

  private final MSCRMObjectService getMSCRMObjectService(final String objectType) {

    logger.debug("---Inside getMSCRMObjectService objectType: " + objectType);
    if (objectType.equalsIgnoreCase(MSCRMObjectType.Account.toString())) {
      return mSCRMV5AccountService;
    } else if (objectType.equalsIgnoreCase(MSCRMObjectType.Task.toString())) {
      return mSCRMV5TaskService;
    } else if (objectType.equalsIgnoreCase(MSCRMObjectType.Contact.toString())) {
      return mSCRMV5ContactService;
    } else if (objectType.equalsIgnoreCase(MSCRMObjectType.Lead.toString())) {
      return mSCRMV5LeadService;
    } else if (objectType.equalsIgnoreCase(MSCRMObjectType.Opportunity.toString())) {
      return mSCRMV5OpportunityService;
    } else if (objectType.equalsIgnoreCase(MSCRMObjectType.Incident.toString())) {
      return mSCRMV5IncidentService;
    } else if (objectType.equalsIgnoreCase(MSCRMObjectType.Case.toString())) {
      return mSCRMV5CaseService;
    } else {
      throw new CADException(CADResponseCodes._1003 + "Following object type is not supported by the CAD");
    }
  }

  /**
   * @return the applicationContext
   */
  public final ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  /**
   * @param applicationContext the applicationContext to set
   */
  public final void setApplicationContext(final ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * @return the mSCRMV5AccountService
   */
  public final MSCRMV5AccountService getmSCRMV5AccountService() {
    return this.mSCRMV5AccountService;
  }

  /**
   * @param mSCRMV5AccountService the mSCRMV5AccountService to set
   */
  public final void setmSCRMV5AccountService(final MSCRMV5AccountService mSCRMV5AccountService) {
    this.mSCRMV5AccountService = mSCRMV5AccountService;
  }

  /**
   * @return the mSCRMV5TaskService
   */
  public final MSCRMV5TaskService getmSCRMV5TaskService() {
    return this.mSCRMV5TaskService;
  }

  /**
   * @param mSCRMV5TaskService the mSCRMV5TaskService to set
   */
  public final void setmSCRMV5TaskService(final MSCRMV5TaskService mSCRMV5TaskService) {
    this.mSCRMV5TaskService = mSCRMV5TaskService;
  }

  /**
   * @return the mSCRMV5ContactService
   */
  public final MSCRMV5ContactService getmSCRMV5ContactService() {
    return this.mSCRMV5ContactService;
  }

  /**
   * @param mSCRMV5ContactService the mSCRMV5ContactService to set
   */
  public final void setmSCRMV5ContactService(final MSCRMV5ContactService mSCRMV5ContactService) {
    this.mSCRMV5ContactService = mSCRMV5ContactService;
  }

  /**
   * @return the mSCRMV5LeadService
   */
  public final MSCRMV5LeadService getmSCRMV5LeadService() {
    return this.mSCRMV5LeadService;
  }

  /**
   * @param mSCRMV5LeadService the mSCRMV5LeadService to set
   */
  public final void setmSCRMV5LeadService(final MSCRMV5LeadService mSCRMV5LeadService) {
    this.mSCRMV5LeadService = mSCRMV5LeadService;
  }

  /**
   * @return the mSCRMV5IncidentService
   */
  public final MSCRMV5IncidentService getmSCRMV5IncidentService() {
    return this.mSCRMV5IncidentService;
  }

  /**
   * @param mSCRMV5IncidentService the mSCRMV5IncidentService to set
   */
  public final void setmSCRMV5IncidentService(final MSCRMV5IncidentService mSCRMV5IncidentService) {
    this.mSCRMV5IncidentService = mSCRMV5IncidentService;
  }

  /**
   * @return the mSCRMV5CaseService
   */
  public final MSCRMV5CaseService getmSCRMV5CaseService() {
    return this.mSCRMV5CaseService;
  }

  /**
   * @param mSCRMV5CaseService the mSCRMV5CaseService to set
   */
  public final void setmSCRMV5CaseService(final MSCRMV5CaseService mSCRMV5CaseService) {
    this.mSCRMV5CaseService = mSCRMV5CaseService;
  }

  /**
   * @return the mSCRMV5OpportunityService
   */
  public final MSCRMV5OpportunityService getmSCRMV5OpportunityService() {
    return this.mSCRMV5OpportunityService;
  }

  /**
   * @param mSCRMV5OpportunityService the mSCRMV5OpportunityService to set
   */
  public final void setmSCRMV5OpportunityService(final MSCRMV5OpportunityService mSCRMV5OpportunityService) {
    this.mSCRMV5OpportunityService = mSCRMV5OpportunityService;
  }

}
