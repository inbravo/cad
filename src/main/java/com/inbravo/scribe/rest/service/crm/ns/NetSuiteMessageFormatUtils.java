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

package com.inbravo.scribe.rest.service.crm.ns;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.inbravo.scribe.exception.ScribeException;
import com.inbravo.scribe.exception.ScribeResponseCodes;
import com.inbravo.scribe.rest.resource.ScribeCommandObject;
import com.inbravo.scribe.rest.resource.ScribeObject;
import com.inbravo.scribe.rest.service.crm.CRMMessageFormatUtils;
import com.netsuite.webservices.platform.core.BooleanCustomFieldRef;
import com.netsuite.webservices.platform.core.CustomFieldList;
import com.netsuite.webservices.platform.core.CustomFieldRef;
import com.netsuite.webservices.platform.core.DateCustomFieldRef;
import com.netsuite.webservices.platform.core.ListOrRecordRef;
import com.netsuite.webservices.platform.core.LongCustomFieldRef;
import com.netsuite.webservices.platform.core.MultiSelectCustomFieldRef;
import com.netsuite.webservices.platform.core.Record;
import com.netsuite.webservices.platform.core.RecordList;
import com.netsuite.webservices.platform.core.RecordRef;
import com.netsuite.webservices.platform.core.SearchResult;
import com.netsuite.webservices.platform.core.SelectCustomFieldRef;
import com.netsuite.webservices.platform.core.Status;
import com.netsuite.webservices.platform.core.StatusDetail;
import com.netsuite.webservices.platform.core.StringCustomFieldRef;

/**
 * 
 * @author amit.dixit
 * 
 */
public final class NetSuiteMessageFormatUtils extends CRMMessageFormatUtils {

  private static final Logger logger = Logger.getLogger(NetSuiteMessageFormatUtils.class.getName());

  private NetSuiteMessageFormatUtils() {

  }

  /* These are the CRM fields of NS */
  public final static String NS_WSDL_PACKAGE_NAME = "com.netsuite.webservices.platform.core.";

  /**
   * 
   * @param cADCommandObject
   * @param searchResult
   * @return
   * @throws Exception
   */
  @SuppressWarnings("unused")
  public static final ScribeCommandObject processResponse(final ScribeCommandObject cADCommandObject, final SearchResult searchResult,
      final Set<String> crmFieldToBeSelectedSet) throws Exception {

    logger.debug("----Inside processResponse crmFieldToBeSelectedSet size: " + crmFieldToBeSelectedSet.size());

    if (searchResult == null) {

      /* Inform use about no response from NS */
      throw new ScribeException(ScribeResponseCodes._1004 + "No records found from NS");
    }

    /* Get record list */
    final RecordList recordList = searchResult.getRecordList();

    /* Check if record list is null */
    if (recordList == null) {

      /* Get status */
      final Status status = searchResult.getStatus();

      if (status != null) {

        /* Get status detail */
        final StatusDetail[] statusDetail = status.getStatusDetail();

        final StringBuffer stringBuffer = new StringBuffer();

        /* Get message from response */
        for (int i = 0; i < statusDetail.length; i++) {
          stringBuffer.append(statusDetail[i].getType().getValue() + ":" + statusDetail[i].getMessage());
        }

        /* Inform use about message from NS */
        throw new ScribeException(ScribeResponseCodes._1004 + "No records found from NS : " + stringBuffer);
      } else {
        /* Inform use about no records from NS */
        throw new ScribeException(ScribeResponseCodes._1004 + "No records found from NS");
      }
    }

    /* Get records */
    final Record[] records = recordList.getRecord();

    /* Create page index variable */
    int pageIndex = 0;

    /* Create Scribe Object list */
    final List<ScribeObject> cADbjectList = new ArrayList<ScribeObject>();

    /* Check if records are not null */
    if (records != null) {

      /* Get page index from response */
      if (searchResult.getPageIndex() != null) {
        pageIndex = searchResult.getPageIndex().intValue();
      }

      /* Iterate over records */
      for (int i = 0, j = (pageIndex - 1) * 500; i < records.length; i++, j++) {

        /* Create one Object each record */
        final ScribeObject cADbject = new ScribeObject();

        /* Create list of elements */
        final List<Element> elementList = new ArrayList<Element>();

        /* Use reflection to set the vallues in customer */
        final Method[] objectMethods = records[i].getClass().getMethods();

        /* Iterate over array and find the getter */
        for (int k = 0; k < objectMethods.length; k++) {

          /* Get method name */
          final String methodName = objectMethods[k].getName();

          /* Create parameter array */
          final Object[] parameter = new Object[objectMethods[k].getParameterTypes().length];

          /* Variable to mark if custom field is found */
          boolean customFieldFound = false;

          /* Check if method starts with get */
          if (methodName.startsWith("get")) {

            try {
              /* Invoke the method */
              final Object object = objectMethods[k].invoke(records[i], parameter);

              /* Get element name */
              final String elementName = methodName.substring(3, methodName.length());

              String crmFieldValue = null;

              /* Check if object is not string */
              if (object instanceof RecordRef) {

                /* Get node value */
                final RecordRef recordRef = (RecordRef) object;

                crmFieldValue = recordRef.getInternalId();
              } else if (object instanceof String) {

                /* Convert it to string */
                crmFieldValue = (String) object;
              } else if (object instanceof Boolean) {

                if (((Boolean) object).equals(Boolean.TRUE)) {

                  /* Convert it to boolean */
                  crmFieldValue = Boolean.TRUE.toString();
                } else {
                  /* Convert it to boolean */
                  crmFieldValue = Boolean.FALSE.toString();
                }
              } else if (object instanceof Long) {

                /* Convert it to string */
                crmFieldValue = ((Long) object).toString();
              } else if (object instanceof Double) {

                /* Convert it to string */
                crmFieldValue = ((Double) object).toString();
              } else if (object instanceof GregorianCalendar) {

                /* Convert it to date */
                final GregorianCalendar gregorianCalendar = (GregorianCalendar) object;

                crmFieldValue = gregorianCalendar.getTime().toString();

              } else if (object instanceof ListOrRecordRef) {

                /* Convert it to list record */
                final ListOrRecordRef ob = (ListOrRecordRef) object;

                crmFieldValue = ob.getInternalId();
              } else if (object instanceof CustomFieldList) {

                /* Process custom fields */
                processCustomFields(object, elementList, crmFieldToBeSelectedSet);

                /* mark that custom field is found */
                customFieldFound = true;
              }

              /* Add only if not a custom field */
              if (!customFieldFound) {

                /* Check if field is asked in request */
                if (crmFieldToBeSelectedSet.size() == 0 || crmFieldToBeSelectedSet.contains(elementName)) {

                  /* Create XML element for the DB record */
                  elementList.add(CRMMessageFormatUtils.createMessageElement(elementName, crmFieldValue));
                }
              }

            } catch (final IllegalArgumentException e) {
              throw new ScribeException(ScribeResponseCodes._1003 + "Found problem in processing CRM fields in response from NS");
            } catch (final IllegalAccessException e) {
              throw new ScribeException(ScribeResponseCodes._1003 + "Found problem in processing CRM fields in response from NS");
            } catch (final InvocationTargetException e) {
              throw new ScribeException(ScribeResponseCodes._1003 + "Found problem in processing CRM fields in response from NS");
            }
          }
        }

        /* Add all CRM fields */
        cADbject.setXmlContent(elementList);

        /* Set the object at array */
        cADbjectList.add(cADbject);
      }
    } else {
      /* Inform use about no records from NS */
      throw new ScribeException(ScribeResponseCodes._1004 + "No records found from NS");
    }
    logger.debug("----Inside processCustomerInResponse records count in response: " + cADbjectList.size());

    /* Set the final object in command object */
    cADCommandObject.setObject(cADbjectList.toArray(new ScribeObject[cADbjectList.size()]));

    return cADCommandObject;
  }

  /**
   * 
   * @param object
   * @param elementList
   * @return
   * @throws Exception
   */
  private static final List<Element> processCustomFields(final Object object, final List<Element> elementList,
      final Set<String> crmFieldToBeSelectedSet) throws Exception {

    logger.debug("----Inside processCustomFields");

    /* Type cast the obejct to list */
    final CustomFieldList customFieldList = (CustomFieldList) object;

    if (customFieldList != null) {

      final CustomFieldRef[] customFieldRefArr = customFieldList.getCustomField();

      if (customFieldRefArr != null) {

        /* Get all custom fields */
        for (int l = 0; l < customFieldRefArr.length; l++) {

          logger.debug("----Inside processCustomFields customFieldRefArr[l]" + customFieldRefArr[l]);

          if (customFieldRefArr[l] instanceof SelectCustomFieldRef) {

            final SelectCustomFieldRef selectCustomFieldRef = (SelectCustomFieldRef) customFieldRefArr[l];

            /* Check if field is asked in request */
            if (crmFieldToBeSelectedSet.size() == 0 || crmFieldToBeSelectedSet.contains(selectCustomFieldRef.getInternalId())) {

              /* Create XML element */
              elementList.add(CRMMessageFormatUtils.createMessageElement(selectCustomFieldRef.getInternalId(), selectCustomFieldRef.getValue()
                  .getInternalId()));
            }

          } else if (customFieldRefArr[l] instanceof MultiSelectCustomFieldRef) {

            final MultiSelectCustomFieldRef multiSelectCustomFieldRef = (MultiSelectCustomFieldRef) customFieldRefArr[l];

            /* Check if field is asked in request */
            if (crmFieldToBeSelectedSet.size() == 0 || crmFieldToBeSelectedSet.contains(multiSelectCustomFieldRef.getInternalId())) {
              /* Create XML element */
              elementList.add(CRMMessageFormatUtils.createMessageElement(multiSelectCustomFieldRef.getInternalId(),
                  multiSelectCustomFieldRef.getValue()));

            }

          } else if (customFieldRefArr[l] instanceof StringCustomFieldRef) {

            final StringCustomFieldRef stringCustomFieldRef = (StringCustomFieldRef) customFieldRefArr[l];

            /* Check if field is asked in request */
            if (crmFieldToBeSelectedSet.size() == 0 || crmFieldToBeSelectedSet.contains(stringCustomFieldRef.getInternalId())) {
              /* Create XML element */
              elementList.add(CRMMessageFormatUtils.createMessageElement(stringCustomFieldRef.getInternalId(), stringCustomFieldRef.getValue()));
            }

          } else if (customFieldRefArr[l] instanceof LongCustomFieldRef) {

            final LongCustomFieldRef longCustomFieldRef = (LongCustomFieldRef) customFieldRefArr[l];

            /* Check if field is asked in request */
            if (crmFieldToBeSelectedSet.size() == 0 || crmFieldToBeSelectedSet.contains(longCustomFieldRef.getInternalId())) {

              /* Create XML element */
              elementList.add(CRMMessageFormatUtils.createMessageElement(longCustomFieldRef.getInternalId(), longCustomFieldRef.getValue()));

            }

          } else if (customFieldRefArr[l] instanceof DateCustomFieldRef) {

            final DateCustomFieldRef dateCustomFieldRef = (DateCustomFieldRef) customFieldRefArr[l];

            /* If value is not null */
            if (dateCustomFieldRef.getValue() != null) {

              /* Convert it to date */
              final GregorianCalendar gregorianCalendar = (GregorianCalendar) dateCustomFieldRef.getValue();

              /* Check if field is asked in request */
              if (crmFieldToBeSelectedSet.size() == 0 || crmFieldToBeSelectedSet.contains(dateCustomFieldRef.getInternalId())) {
                /* Create XML element */
                elementList.add(CRMMessageFormatUtils.createMessageElement(dateCustomFieldRef.getInternalId(), gregorianCalendar.getTime()));
              }

            } else {
              /* Check if field is asked in request */
              if (crmFieldToBeSelectedSet.size() == 0 || crmFieldToBeSelectedSet.contains(dateCustomFieldRef.getInternalId())) {

                /* Create XML element */
                elementList.add(CRMMessageFormatUtils.createMessageElement(dateCustomFieldRef.getInternalId(), ""));
              }
            }

          } else if (customFieldRefArr[l] instanceof BooleanCustomFieldRef) {

            final BooleanCustomFieldRef booleanCustomFieldRef = (BooleanCustomFieldRef) customFieldRefArr[l];

            /* Check if field is asked in request */
            if (crmFieldToBeSelectedSet.size() == 0 || crmFieldToBeSelectedSet.contains(booleanCustomFieldRef.getInternalId())) {

              /* Create XML element */
              elementList.add(CRMMessageFormatUtils.createMessageElement(booleanCustomFieldRef.getInternalId(), booleanCustomFieldRef.isValue()));
            }
          }
        }
      }
    }
    return elementList;
  }
}
