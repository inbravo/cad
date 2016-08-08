/*
 * MIT License
 * 
 * Copyright (c) 2016 Amit Dixit (github.com/inbravo)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.inbravo.scribe.rest.service.crm.sf.session;

import org.apache.axis.message.SOAPHeaderElement;
import org.apache.log4j.Logger;

import com.inbravo.scribe.exception.CADException;
import com.inbravo.scribe.exception.CADResponseCodes;
import com.inbravo.scribe.internal.service.dto.BasicObject;
import com.inbravo.scribe.internal.service.dto.CADUser;
import com.inbravo.scribe.rest.service.crm.cache.CRMSessionCache;
import com.inbravo.scribe.rest.service.crm.session.CRMSessionManager;
import com.inbravo.scribe.rest.service.crm.sf.SalesForceSOAPClient;
import com.sforce.soap.partner.SforceServiceLocator;
import com.sforce.soap.partner.SoapBindingStub;

/**
 * 
 * @author amit.dixit
 * 
 */
public class SalesForceCRMSessionManager implements CRMSessionManager {

  private final Logger logger = Logger.getLogger(SalesForceCRMSessionManager.class.getName());

  /* CRM session cache */
  private CRMSessionCache cRMSessionCache;

  /* Sales Force SOAP client */
  private SalesForceSOAPClient salesForceSOAPClient;

  private String agentIdSplitCharacter;

  public final synchronized SoapBindingStub getSoapBindingStub(final String crmUserId, final String crmPassword) throws Exception {

    logger.debug("---Inside getSoapBindingStub: " + crmUserId);

    /* Recover agent from cache */
    final CADUser user = (CADUser) cRMSessionCache.recover(crmUserId);

    SoapBindingStub soapBindingStub = null;

    /* This code block will be usefull if cache size limit is reached */
    if (user != null) {

      logger.debug("---Inside tenant not found in cache hence going for fresh fetch. Seems like cache limit is reached");

      /* Login at Sales Force */
      soapBindingStub = salesForceSOAPClient.login(user.getCrmUserId(), user.getCrmPassword());
    } else {

      /* Login at Sales Force */
      soapBindingStub = salesForceSOAPClient.login(crmUserId, crmPassword);
    }

    /* Take session information from agent */
    final SOAPHeaderElement sOAPHeaderElement =
        soapBindingStub.getHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(), "SessionHeader");

    if (sOAPHeaderElement != null) {

      logger.debug("---Inside getSoapBindingStub user's sessionId : " + sOAPHeaderElement.getAsDOM().getFirstChild().getTextContent());

      /* Set session information at agent */
      /* Following session will be used for pagination */
      user.setCrmSessionId(sOAPHeaderElement.getAsDOM().getFirstChild().getTextContent());
    } else {
      /* Inform user about absent header value */
      throw new CADException(CADResponseCodes._1008 + "CRM session id not set with cache object");
    }

    /* Re-admit this agent with CRM session information */
    cRMSessionCache.admit(crmUserId, user);

    return soapBindingStub;
  }

  /**
   * 
   * @param crmUserId
   * @param crmPassword
   * @return
   * @throws Exception
   */
  public final boolean reset(final String crmUserId, final String crmPassword) throws Exception {

    /* Forward to login */
    return this.login(crmUserId, crmPassword);
  }

  /**
   * 
   * @param id
   * @return
   * @throws Exception
   */

  @SuppressWarnings("unused")
  public final boolean login(final String crmUserId, final String crmPassword) throws Exception {

    /* Check if session is already available at cache */
    final BasicObject basicObject = (BasicObject) cRMSessionCache.recover(crmUserId);


    /* Recover agent from cache */
    final CADUser user = (CADUser) cRMSessionCache.recover(crmUserId);

    SoapBindingStub soapBindingStub = null;

    /* This code block will be usefull if cache size limit is reached */
    if (user != null) {

      logger.debug("---Inside login user not found in cache hence going for fresh fetch. Seems like cache limit is reached");

      /* Login at Sales Force */
      soapBindingStub = salesForceSOAPClient.login(user.getCrmUserId(), user.getCrmPassword());
    } else {

      /* Login at Sales Force */
      soapBindingStub = salesForceSOAPClient.login(crmUserId, crmPassword);
    }

    /* Take session information from agent */
    final SOAPHeaderElement sOAPHeaderElement =
        soapBindingStub.getHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(), "SessionHeader");

    if (sOAPHeaderElement != null) {

      logger.debug("---Inside reset user's sessionId : " + sOAPHeaderElement.getAsDOM().getFirstChild().getTextContent());

      /* Set session information at agent */
      /* Following session will be used for pagination */
      user.setCrmSessionId(sOAPHeaderElement.getAsDOM().getFirstChild().getTextContent());
    } else {
      /* Inform user about absent header value */
      throw new CADException(CADResponseCodes._1008 + "CRM session id not set with cache object");
    }

    /* Save this freshly updated tenant in cache */
    cRMSessionCache.admit(crmUserId, user);

    /* If everything is fine return true */
    return true;
  }

  @Override
  public final BasicObject getSessionInfo(final String id) throws Exception {
    throw new CADException(CADResponseCodes._1003 + "Following operation is not supported by the CRM");
  }

  public final CRMSessionCache getcRMSessionCache() {
    return cRMSessionCache;
  }

  public final void setcRMSessionCache(final CRMSessionCache cRMSessionCache) {
    this.cRMSessionCache = cRMSessionCache;
  }

  public final void setSalesForceSOAPClient(final SalesForceSOAPClient salesForceSOAPClient) {
    this.salesForceSOAPClient = salesForceSOAPClient;
  }

  public final SalesForceSOAPClient getSalesForceSOAPClient() {
    return salesForceSOAPClient;
  }

  public final String getCrmUserIdIdSplitCharacter() {
    return agentIdSplitCharacter;
  }

  public final void setCrmUserIdIdSplitCharacter(final String agentIdSplitCharacter) {
    this.agentIdSplitCharacter = agentIdSplitCharacter;
  }
}