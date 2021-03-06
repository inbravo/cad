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

package com.inbravo.scribe.rest.service.crm;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.axis.message.MessageElement;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.inbravo.scribe.exception.ScribeException;
import com.inbravo.scribe.exception.ScribeResponseCodes;
import com.inbravo.scribe.rest.resource.ScribeCommandObject;
import com.inbravo.scribe.rest.resource.ScribeObject;
import com.inbravo.scribe.rest.service.crm.sf.SalesForceMessageFormatUtils;
import com.inbravo.scribe.rest.service.crm.sf.session.SalesForceCRMSessionManager;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.SearchRecord;
import com.sforce.soap.partner.SearchResult;
import com.sforce.soap.partner.SoapBindingStub;
import com.sforce.soap.partner.sobject.SObject;

/**
 * 
 * @author amit.dixit
 * 
 */
public final class SFSOAPCRMService extends CRMService {

  private final Logger logger = Logger.getLogger(SFSOAPCRMService.class.getName());

  private SalesForceCRMSessionManager cRMSessionManager;

  private String crmFieldsSeparator;

  private String orderFieldsSeparator;

  @Override
  public final ScribeCommandObject createObject(final ScribeCommandObject cADCommandObject) throws Exception {

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    /* Create message element array */
    final MessageElement[] messageElementArray = SalesForceMessageFormatUtils.createMessageElementArray(cADCommandObject.getObject()[0]);

    /* Create Sales object of type 'Account' */
    final SObject sObject = new SObject();
    sObject.setType(cADCommandObject.getObjectType());
    sObject.set_any(messageElementArray);

    /* Find start time */
    final long startTime = System.currentTimeMillis();

    /* Create SalesForce object */
    final SaveResult[] saveResults = soapBindingStub.create(new SObject[] {sObject});

    /* Find end time */
    final long endTime = System.currentTimeMillis();

    /* Calculate processing time by Sales Force */
    logger.debug("----Inside createObject time taken in Sales Force processing: " + ((endTime - startTime)) + " msec(s)");
    if (saveResults != null) {
      for (int i = 0; i < saveResults.length; i++) {
        if (saveResults[i].isSuccess()) {
          logger.debug("----Inside createObject object with id: " + saveResults[i].getId() + " is created");

          /* Set object id in object before sending back */
          cADCommandObject.getObject()[0] =
              SalesForceMessageFormatUtils.setNodeValue("Id", saveResults[i].getId(), cADCommandObject.getObject()[0]);
        } else {
          logger.info("----Inside createObject: error recieved from Sales Force: " + saveResults[i].getErrors()[0].getMessage());
          throw new ScribeException(ScribeResponseCodes._1001 + cADCommandObject.getObjectType() + " : Message : "
              + saveResults[i].getErrors()[0].getMessage() + " : Status Code : " + saveResults[i].getErrors()[0].getStatusCode() + " : Fields : "
              + saveResults[i].getErrors()[0].getFields());
        }
      }
    } else {
      throw new ScribeException(ScribeResponseCodes._1001 + cADCommandObject.getObjectType());
    }
    return cADCommandObject;
  }

  @Override
  public final boolean deleteObject(final ScribeCommandObject cADCommandObject, final String idToBeDeleted) throws Exception {

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    /* Find start time */
    final long startTime = System.currentTimeMillis();

    /* Call SalesForce */
    final DeleteResult[] deleteResult = soapBindingStub.delete(new String[] {idToBeDeleted});

    /* Find end time */
    final long endTime = System.currentTimeMillis();

    /* Calculate processing time by Sales Force */
    logger.debug("----Inside deleteObject time taken in Sales Force processing: " + ((endTime - startTime)) + " msec(s)");
    if (deleteResult != null) {
      for (int i = 0; i < deleteResult.length; i++) {
        if (deleteResult[i].isSuccess()) {
          logger.debug("----Inside deleteObject object with id: " + deleteResult[i].getId() + " is deleted");
        } else {
          logger.info("----Inside deleteObject: error recieved from Sales Force: " + deleteResult[i].getErrors()[0].getMessage());
          throw new ScribeException(ScribeResponseCodes._1001 + cADCommandObject.getObjectType() + " : Message : "
              + deleteResult[i].getErrors()[0].getMessage() + " : Status Code : " + deleteResult[i].getErrors()[0].getStatusCode() + " : Fields : "
              + deleteResult[i].getErrors()[0].getFields());
        }
      }
    } else {
      throw new ScribeException(ScribeResponseCodes._1001 + cADCommandObject.getObjectType());
    }
    /* Control will arrive to this return only after the success */
    return true;
  }

  @Override
  public final ScribeCommandObject getObjects(final ScribeCommandObject cADCommandObject) throws Exception {

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    QueryResult queryResult = null;

    /* Check if batch is required */
    if (cADCommandObject.getBatch() != null) {
      logger.debug("----Inside getObjects going to fetch a batch with query locator: " + cADCommandObject.getBatch());
      /* Query Sales Force with batch information */
      queryResult = soapBindingStub.queryMore(cADCommandObject.getBatch());
    } else {
      /* Make a Sales Force query to know the Object Fields */
      final DescribeSObjectResult describeSObjectResult = soapBindingStub.describeSObject(cADCommandObject.getObjectType());

      /* Create a string object for query */
      String query = null;

      /* Iterate over records */
      if (describeSObjectResult != null) {

        if (describeSObjectResult.getFields() != null) {
          query = "Select ";
          for (int i = 0; i < describeSObjectResult.getFields().length; i++) {
            final Field field = describeSObjectResult.getFields()[i];
            query = query + field.getName() + " , ";
          }

          /* Remove comma (,) from the end of string */
          query = query.substring(0, query.length() - 2);

          /* Add 'From' clause */
          query = query + " From " + cADCommandObject.getObjectType();
        } else {
          logger.debug("----Inside getObjects no records in response");
          throw new ScribeException(ScribeResponseCodes._1004 + "Any " + cADCommandObject.getObjectType() + " at Sales Force");
        }
      } else {
        logger.debug("----Inside getObjects no response from Sales Force");
        throw new ScribeException(ScribeResponseCodes._1005);
      }

      logger.debug("----Inside getObjects SOQL query: " + query);

      /* Query Sales Force without batch information */
      queryResult = soapBindingStub.query(query);
    }

    /* Iterate over records */
    if (queryResult != null) {

      if (queryResult.getRecords() != null) {

        /* Create Object array with query result length */
        final ScribeObject[] cADbjectArray = new ScribeObject[queryResult.getRecords().length];
        for (int i = 0; i < queryResult.getRecords().length; i++) {

          /* Create one Object each record */
          final ScribeObject cADbject = new ScribeObject();

          /* Get record number i */
          final SObject sObject = queryResult.getRecords()[i];

          /* Get all fields from the record */
          final MessageElement[] fields = sObject.get_any();

          /* Set object type */
          cADbject.setObjectType(sObject.getType());

          /* Create list of elements */
          final List<Element> elementList = new ArrayList<Element>();

          for (int j = 0; j < fields.length; j++) {

            /* Create inner tags <Field_Name> inside <Object> */
            elementList.add(SalesForceMessageFormatUtils.createMessageElement(fields[j].getName(), fields[j].getValue()));
          }

          /* Add all CRM fields */
          cADbject.setXmlContent(elementList);

          /* Set the object at array */
          cADbjectArray[i] = cADbject;
        }
        logger.debug("----Inside getObjects object length: " + cADbjectArray.length);

        /* Add batch id(query locator) information for pagination */
        if (queryResult.getQueryLocator() != null) {
          cADCommandObject.setBatch(queryResult.getQueryLocator());
        } else {
          cADCommandObject.setBatch(null);
        }

        /* Set the final object in command object */
        cADCommandObject.setObject(cADbjectArray);
      } else {
        logger.debug("----Inside getObjects no records in response");
        throw new ScribeException(ScribeResponseCodes._1004 + cADCommandObject.getObjectType());
      }
    } else {
      logger.debug("----Inside getObjects no response from Sales Force");
      throw new ScribeException(ScribeResponseCodes._1005);
    }
    return cADCommandObject;
  }

  private final ScribeCommandObject getObjectsBySelectCriteria(final ScribeCommandObject cADCommandObject, final String selectCriteria) throws Exception {

    logger.debug("----Inside getObjectsBySelectCriteria selectCriteria: " + selectCriteria);

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());


    /* Query Sales Force without batch information */
    final QueryResult queryResult = soapBindingStub.query(selectCriteria);

    /* Iterate over records */
    if (queryResult != null) {

      if (queryResult.getRecords() != null) {

        /* Create Object array with query result length */
        final ScribeObject[] cADbjectArray = new ScribeObject[queryResult.getRecords().length];

        for (int i = 0; i < queryResult.getRecords().length; i++) {

          /* Create one Object each record */
          final ScribeObject cADbject = new ScribeObject();

          /* Get record number i */
          final SObject sObject = queryResult.getRecords()[i];

          /* Get all fields from the record */
          final MessageElement[] fields = sObject.get_any();

          /* Set object type */
          cADbject.setObjectType(sObject.getType());

          /* Create list of elements */
          final List<Element> elementList = new ArrayList<Element>();

          for (int j = 0; j < fields.length; j++) {

            /* Create inner tags <Field_Name> inside <Object> */
            elementList.add(SalesForceMessageFormatUtils.createMessageElement(fields[j].getName(), fields[j].getValue()));
          }

          /* Add all CRM fields */
          cADbject.setXmlContent(elementList);

          /* Set the object at array */
          cADbjectArray[i] = cADbject;
        }
        logger.debug("----Inside getObjectsBySelectCriteria object length: " + cADbjectArray.length);

        /* Add batch id(query locator) information for pagination */
        if (queryResult.getQueryLocator() != null) {
          cADCommandObject.setBatch(queryResult.getQueryLocator());
        } else {
          cADCommandObject.setBatch(null);
        }

        /* Set the final object in command object */
        cADCommandObject.setObject(cADbjectArray);
      } else {
        logger.debug("----Inside getObjectsBySelectCriteria no records in response");
        throw new ScribeException(ScribeResponseCodes._1004 + cADCommandObject.getObjectType());
      }
    } else {
      logger.debug("----Inside getObjectsBySelectCriteria no response from Sales Force");
      throw new ScribeException(ScribeResponseCodes._1005);
    }
    return cADCommandObject;
  }

  @Override
  public final ScribeCommandObject getObjects(final ScribeCommandObject cADCommandObject, final String query) throws Exception {
    logger.debug("----Inside getObjects query: " + query);

    /* Check if query is a SFDC find type */
    if (query != null && query.trim().startsWith("find")) {
      return this.getObjectsBySearchCriteria(cADCommandObject, query);
    }
    /* Check if query is a SFDC select type */
    else if (query != null && query.trim().startsWith("select")) {
      return this.getObjectsBySelectCriteria(cADCommandObject, query);
    }

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    QueryResult queryResult = null;

    /* Check if batch is required */
    if (cADCommandObject.getBatch() != null) {
      logger.debug("----Inside getObjects going to fetch a batch with query locator: " + cADCommandObject.getBatch());

      /* Query Sales Force with batch information */
      queryResult = soapBindingStub.queryMore(cADCommandObject.getBatch());
    } else {

      /* Make a Sales Force query to know the Object Fields */
      final DescribeSObjectResult describeSObjectResult = soapBindingStub.describeSObject(cADCommandObject.getObjectType());

      /* Create a string object for query */
      String cadQuery = null;

      /* Iterate over records */
      if (describeSObjectResult != null) {

        if (describeSObjectResult.getFields() != null) {
          cadQuery = "Select ";
          for (int i = 0; i < describeSObjectResult.getFields().length; i++) {
            Field field = describeSObjectResult.getFields()[i];
            cadQuery = cadQuery + field.getName() + " , ";
          }

          /* Remove comma (,) from the end of string */
          cadQuery = cadQuery.substring(0, cadQuery.length() - 2);

          /* Add 'From' clause */
          cadQuery = cadQuery + " From " + cADCommandObject.getObjectType();

          /* Add where clause */
          if (query != null && !query.equalsIgnoreCase("NONE")) {

            /* Create where clause */
            final String whereClause = SalesForceMessageFormatUtils.createWhereClause(query);

            /* Complete the cadQuery */
            cadQuery = cadQuery + whereClause;
          }
        } else {
          logger.debug("----Inside getObjects no records in response");
          throw new ScribeException(ScribeResponseCodes._1004 + "Any " + cADCommandObject.getObjectType() + " at Sales Force");
        }
      } else {
        logger.debug("----Inside getObjects no response from Sales Force");
        throw new ScribeException(ScribeResponseCodes._1005);
      }

      logger.debug("----Inside getObjects SOQL query: " + cadQuery);

      /* Query Sales Force without batch information */
      queryResult = soapBindingStub.query(cadQuery);
    }

    /* Iterate over records */
    if (queryResult != null) {

      if (queryResult.getRecords() != null) {

        /* Create Object array with query result length */
        final ScribeObject[] cADbjectArray = new ScribeObject[queryResult.getRecords().length];

        for (int i = 0; i < queryResult.getRecords().length; i++) {

          /* Create one Object each record */
          final ScribeObject cADbject = new ScribeObject();

          /* Get record number i */
          final SObject sObject = queryResult.getRecords()[i];

          /* Get all fields from the record */
          final MessageElement[] fields = sObject.get_any();

          /* Set object type */
          cADbject.setObjectType(sObject.getType());

          /* Create list of elements */
          final List<Element> elementList = new ArrayList<Element>();

          for (int j = 0; j < fields.length; j++) {

            /* Create inner tags <Field_Name> inside <Object> */
            elementList.add(SalesForceMessageFormatUtils.createMessageElement(fields[j].getName(), fields[j].getValue()));
          }

          /* Add all CRM fields */
          cADbject.setXmlContent(elementList);

          /* Set the object at array */
          cADbjectArray[i] = cADbject;
        }
        logger.debug("----Inside getObjects object length: " + cADbjectArray.length);

        /* Add batch id(query locator) information for pagination */
        if (queryResult.getQueryLocator() != null) {
          cADCommandObject.setBatch(queryResult.getQueryLocator());
        } else {
          cADCommandObject.setBatch(null);
        }

        /* Set the final object in command object */
        cADCommandObject.setObject(cADbjectArray);
      } else {
        logger.debug("----Inside getObjects no records in response");
        throw new ScribeException(ScribeResponseCodes._1004 + cADCommandObject.getObjectType());
      }
    } else {
      logger.debug("----Inside getObjects no response from Sales Force");
      throw new ScribeException(ScribeResponseCodes._1005);
    }
    return cADCommandObject;
  }

  private final ScribeCommandObject getObjectsBySearchCriteria(final ScribeCommandObject cADCommandObject, final String searchCriteria) throws Exception {
    logger.debug("----Inside getObjectsBySearchCriteria searchCriteria: " + searchCriteria);

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    logger.debug("----Inside getObjectsBySearchCriteria SOSL query: " + searchCriteria);

    /* Search Sales Force without batch information */
    final SearchResult searchResult = soapBindingStub.search(searchCriteria);

    /* Iterate over records */
    if (searchResult != null) {

      if (searchResult.getSearchRecords() != null) {

        /* Create Object array with query result length */
        final ScribeObject[] cADbjectArray = new ScribeObject[searchResult.getSearchRecords().length];

        for (int i = 0; i < searchResult.getSearchRecords().length; i++) {

          /* Create one Object each record */
          final ScribeObject cADbject = new ScribeObject();

          /* Get record number i */
          final SearchRecord searchRecord = searchResult.getSearchRecords()[i];

          /* Get SObject from the record */
          final SObject sObject = searchRecord.getRecord();

          /* Set object type */
          cADbject.setObjectType(sObject.getType());

          /* Get all fields from the SObject */
          final MessageElement[] fields = sObject.get_any();

          /* Create list of elements */
          final List<Element> elementList = new ArrayList<Element>();

          for (int j = 0; j < fields.length; j++) {

            /* Create inner tags <Field_Name> inside <Object> */
            elementList.add(SalesForceMessageFormatUtils.createMessageElement(fields[j].getName(), fields[j].getValue()));
          }

          /* Add all CRM fields */
          cADbject.setXmlContent(elementList);

          /* Set the object at array */
          cADbjectArray[i] = cADbject;
        }
        logger.debug("----Inside getObjectsBySearchCriteria object length: " + cADbjectArray.length);

        /* Set the final object in command object */
        cADCommandObject.setObject(cADbjectArray);
      } else {
        logger.debug("----Inside getObjectsBySearchCriteria no records in response");
        throw new ScribeException(ScribeResponseCodes._1004 + " anything in find query");
      }
    } else {
      logger.debug("----Inside getObjectsBySearchCriteria no response from Sales Force");
      throw new ScribeException(ScribeResponseCodes._1005);
    }
    return cADCommandObject;
  }

  @Override
  public final ScribeCommandObject getObjects(final ScribeCommandObject cADCommandObject, final String query, final String select) throws Exception {
    logger.debug("----Inside getObjects query: " + query + " & select: " + select);

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    QueryResult queryResult = null;

    /* Check if batch is required */
    if (cADCommandObject.getBatch() != null) {
      logger.debug("----Inside getObjects going to fetch a batch with query locator: " + cADCommandObject.getBatch());

      /* Query Sales Force with batch information */
      queryResult = soapBindingStub.queryMore(cADCommandObject.getBatch());
    } else {

      String cadQuery = null;

      /* Iterate over fields to be selected */
      if (select != null) {

        /* Check of request is not select ALL */
        if (!select.equalsIgnoreCase("ALL")) {
          final StringTokenizer stringTokenizer = new StringTokenizer(select, crmFieldsSeparator);

          cadQuery = "Select ";

          /* Iterate on all select fields */
          while (stringTokenizer.hasMoreTokens()) {
            cadQuery = cadQuery + stringTokenizer.nextToken() + " , ";
          }

          /* Remove comma (,) from the end of string */
          cadQuery = cadQuery.substring(0, cadQuery.length() - 2);

          /* Add 'From' clause */
          cadQuery = cadQuery + " From " + cADCommandObject.getObjectType();

          /* Add where clause */
          if (query != null && !query.equalsIgnoreCase("NONE")) {

            /* Create where clause */
            final String sfdcQuery = SalesForceMessageFormatUtils.createWhereClause(query);

            /* Complete the cadQuery */
            cadQuery = cadQuery + sfdcQuery;
          }
        } else {
          /* Make a Sales Force query to know the Object Fields */
          final DescribeSObjectResult describeSObjectResult = soapBindingStub.describeSObject(cADCommandObject.getObjectType());

          /* Iterate over records */
          if (describeSObjectResult != null) {

            if (describeSObjectResult.getFields() != null) {
              cadQuery = "Select ";
              for (int i = 0; i < describeSObjectResult.getFields().length; i++) {
                final Field field = describeSObjectResult.getFields()[i];
                cadQuery = cadQuery + field.getName() + " , ";
              }

              /* Remove comma (,) from the end of string */
              cadQuery = cadQuery.substring(0, cadQuery.length() - 2);

              /* Add 'From' clause */
              cadQuery = cadQuery + " From " + cADCommandObject.getObjectType();

              /* Add where clause */
              if (query != null && !query.equalsIgnoreCase("NONE")) {

                /* Create where clause */
                String sfdcQuery = SalesForceMessageFormatUtils.createWhereClause(query);

                /* Complete the cadQuery */
                cadQuery = cadQuery + sfdcQuery;
              }
            } else {
              logger.debug("----Inside getObjects no records in response");
              throw new ScribeException(ScribeResponseCodes._1004 + "Any " + cADCommandObject.getObjectType() + " at Sales Force");
            }
          } else {
            logger.debug("----Inside getObjects no response from Sales Force");
            throw new ScribeException(ScribeResponseCodes._1005);
          }
        }
      } else {
        logger.debug("----Inside getObjects sSelect fields criteria is empty");
        throw new ScribeException(ScribeResponseCodes._1008 + "Select fields criteria is empty");
      }

      logger.debug("----Inside getObjects SOQL query: " + cadQuery);

      /* Query Sales Force without batch information */
      queryResult = soapBindingStub.query(cadQuery);
    }

    /* Iterate over records */
    if (queryResult != null) {

      if (queryResult.getRecords() != null) {

        /* Create Object array with query result length */
        final ScribeObject[] cADbjectArray = new ScribeObject[queryResult.getRecords().length];

        for (int i = 0; i < queryResult.getRecords().length; i++) {

          /* Create one Object each record */
          final ScribeObject cADbject = new ScribeObject();

          /* Get record number i */
          final SObject sObject = queryResult.getRecords()[i];

          /* Get all fields from the record */
          final MessageElement[] fields = sObject.get_any();

          /* Set object type */
          cADbject.setObjectType(sObject.getType());

          /* Create list of elements */
          final List<Element> elementList = new ArrayList<Element>();

          for (int j = 0; j < fields.length; j++) {

            /* Create inner tags <Field_Name> inside <Object> */
            elementList.add(SalesForceMessageFormatUtils.createMessageElement(fields[j].getName(), fields[j].getValue()));
          }

          /* Add all CRM fields */
          cADbject.setXmlContent(elementList);

          /* Set the object at array */
          cADbjectArray[i] = cADbject;
        }
        logger.debug("----Inside getObjects Object length: " + cADbjectArray.length);

        /* Add batch id(query locator) information for pagination */
        if (queryResult.getQueryLocator() != null) {
          cADCommandObject.setBatch(queryResult.getQueryLocator());
        } else {
          cADCommandObject.setBatch(null);
        }

        /* Set the final object in command object */
        cADCommandObject.setObject(cADbjectArray);
      } else {
        logger.debug("----Inside getObjects no records in response");
        throw new ScribeException(ScribeResponseCodes._1004 + cADCommandObject.getObjectType());
      }
    } else {
      logger.debug("----Inside getObjects no response from Sales Force");
      throw new ScribeException(ScribeResponseCodes._1005);
    }
    return cADCommandObject;
  }

  public final ScribeCommandObject getObjects(final ScribeCommandObject cADCommandObject, final String query, final String select, final String order)
      throws Exception {
    logger.debug("----Inside getObjects query: " + query + " & select: " + select + " & order: " + order);

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    QueryResult queryResult = null;

    /* Check if batch is required */
    if (cADCommandObject.getBatch() != null) {
      logger.debug("----Inside getObjects going to fetch a batch with query locator: " + cADCommandObject.getBatch());

      /* Query Sales Force with batch information */
      queryResult = soapBindingStub.queryMore(cADCommandObject.getBatch());
    } else {

      String cadQuery = null;

      /* Iterate over fields to be selected */
      if (select != null) {

        /* Check of request is not select ALL */
        if (!select.equalsIgnoreCase("ALL")) {
          final StringTokenizer stringTokenizer = new StringTokenizer(select, crmFieldsSeparator);

          cadQuery = "Select ";

          /* Iterate on all select fields */
          while (stringTokenizer.hasMoreTokens()) {
            cadQuery = cadQuery + stringTokenizer.nextToken() + " , ";
          }

          /* Remove comma (,) from the end of string */
          cadQuery = cadQuery.substring(0, cadQuery.length() - 2);

          /* Add 'From' clause */
          cadQuery = cadQuery + " From " + cADCommandObject.getObjectType();

          /* Add where clause */
          if (query != null && !query.equalsIgnoreCase("NONE")) {

            /* Create where clause */
            final String sfdcQuery = SalesForceMessageFormatUtils.createWhereClause(query);

            /* Complete the cadQuery */
            cadQuery = cadQuery + sfdcQuery;
          }
        } else {
          /* Make a Sales Force query to know the Object Fields */
          final DescribeSObjectResult describeSObjectResult = soapBindingStub.describeSObject(cADCommandObject.getObjectType());

          /* Iterate over records */
          if (describeSObjectResult != null) {

            if (describeSObjectResult.getFields() != null) {
              cadQuery = "Select ";
              for (int i = 0; i < describeSObjectResult.getFields().length; i++) {
                final Field field = describeSObjectResult.getFields()[i];
                cadQuery = cadQuery + field.getName() + " , ";
              }

              /* Remove comma (,) from the end of string */
              cadQuery = cadQuery.substring(0, cadQuery.length() - 2);

              /* Add 'From' clause */
              cadQuery = cadQuery + " From " + cADCommandObject.getObjectType();

              /* Add where clause */
              if (query != null && !query.equalsIgnoreCase("NONE")) {

                /* Create where clause */
                final String sfdcQuery = SalesForceMessageFormatUtils.createWhereClause(query);

                /* Complete the cadQuery */
                cadQuery = cadQuery + sfdcQuery;
              }
            } else {
              logger.debug("----Inside getObjects no records in response");
              throw new ScribeException(ScribeResponseCodes._1004 + "Any " + cADCommandObject.getObjectType() + " at Sales Force");
            }
          } else {
            logger.debug("----Inside getObjects no response from Sales Force");
            throw new ScribeException(ScribeResponseCodes._1005);
          }
        }
      } else {
        logger.debug("----Inside getObjects sSelect fields criteria is empty");
        throw new ScribeException(ScribeResponseCodes._1008 + "Select fields criteria is empty");
      }

      /* Create order by clause */
      if (order != null) {
        logger.debug("----Inside getObjects order: " + order);

        /* Check if multiple order are present */
        if (order.contains(",")) {

          /* Tokenize the order string by comma */
          final StringTokenizer stringTokenizer = new StringTokenizer(order, ",");

          /* Add order by clause */
          cadQuery = cadQuery + " order by ";

          /* Iterate on all order by fields */
          while (stringTokenizer.hasMoreTokens()) {
            cadQuery =
                cadQuery + " " + SalesForceMessageFormatUtils.parseAndValidateOrderClause(stringTokenizer.nextToken(), orderFieldsSeparator) + " , ";
          }

          /* Remove comma (,) from the end of string */
          cadQuery = cadQuery.substring(0, cadQuery.length() - 2);
        } else {
          /* Add order by clause */
          cadQuery = cadQuery + " order by ";
          cadQuery = cadQuery + SalesForceMessageFormatUtils.parseAndValidateOrderClause(order, orderFieldsSeparator);
        }
      }
      logger.debug("----Inside getObjects SOQL query: " + cadQuery);

      /* Query Sales Force without batch information */
      queryResult = soapBindingStub.query(cadQuery);
    }

    /* Iterate over records */
    if (queryResult != null) {

      if (queryResult.getRecords() != null) {

        /* Create Object array with query result length */
        final ScribeObject[] cADbjectArray = new ScribeObject[queryResult.getRecords().length];

        for (int i = 0; i < queryResult.getRecords().length; i++) {

          /* Create one Object each record */
          final ScribeObject cADbject = new ScribeObject();

          /* Get record number i */
          final SObject sObject = queryResult.getRecords()[i];

          /* Get all fields from the record */
          final MessageElement[] fields = sObject.get_any();

          /* Set object type */
          cADbject.setObjectType(sObject.getType());

          /* Create list of elements */
          final List<Element> elementList = new ArrayList<Element>();

          for (int j = 0; j < fields.length; j++) {

            /* Create inner tags <Field_Name> inside <Object> */
            elementList.add(SalesForceMessageFormatUtils.createMessageElement(fields[j].getName(), fields[j].getValue()));
          }

          /* Add all CRM fields */
          cADbject.setXmlContent(elementList);

          /* Set the object at array */
          cADbjectArray[i] = cADbject;
        }
        logger.debug("----Inside getObjects Object length: " + cADbjectArray.length);

        /* Add batch id(query locator) information for pagination */
        if (queryResult.getQueryLocator() != null) {
          cADCommandObject.setBatch(queryResult.getQueryLocator());
        } else {
          cADCommandObject.setBatch(null);
        }

        /* Set the final object in command object */
        cADCommandObject.setObject(cADbjectArray);
      } else {
        logger.debug("----Inside getObjects no records in response");
        throw new ScribeException(ScribeResponseCodes._1004 + cADCommandObject.getObjectType());
      }
    } else {
      logger.debug("----Inside getObjects no response from Sales Force");
      throw new ScribeException(ScribeResponseCodes._1005);
    }
    return cADCommandObject;
  }

  @Override
  public final ScribeCommandObject getObjectsCount(final ScribeCommandObject cADCommandObject) throws Exception {
    logger.debug("----Inside getObjectsCount");

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    /* Create a string object for query */
    final String cadQuery = "Select count() from " + cADCommandObject.getObjectType();

    /* Query Sales Force for count of objects */
    final QueryResult queryResult = soapBindingStub.query(cadQuery);

    logger.debug("----Inside getObjectsCount SOQL query: " + cadQuery);

    /* Iterate over records */
    if (queryResult != null) {

      /* Create one Object each record */
      final ScribeObject cADbject = new ScribeObject();

      /* Create list of elements */
      final List<Element> elementList = new ArrayList<Element>();

      /* Create inner tags <Field_Name> inside <Object> */
      elementList.add(SalesForceMessageFormatUtils.createMessageElement("Count", queryResult.getSize()));

      logger.debug("----Inside getObjectsCount objects count: " + queryResult.getSize());

      /* Add all CRM fields */
      cADbject.setXmlContent(elementList);

      /* Set the final object in command object */
      cADCommandObject.setObject(new ScribeObject[] {cADbject});
    } else {
      logger.debug("----Inside getObjectsCount no response from Sales Force");
      throw new ScribeException(ScribeResponseCodes._1005);
    }
    return cADCommandObject;
  }

  @Override
  public final ScribeCommandObject getObjectsCount(final ScribeCommandObject cADCommandObject, final String query) throws Exception {
    logger.debug("----Inside getObjectsCount");

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    /* Create a string object for query */
    String cadQuery = "Select count() from " + cADCommandObject.getObjectType();

    /* Add where clause */
    if (query != null) {

      /* Check if NONE is provided in query clause */
      if (!query.equalsIgnoreCase("NONE")) {
        /* Create where clause */
        final String sfdcQuery = SalesForceMessageFormatUtils.createWhereClause(query);

        /* Complete the cadQuery */
        cadQuery = cadQuery + sfdcQuery;
      } else {
        logger.debug("----Inside getObjectsCount found NONE in where clause: " + cadQuery);
      }
    }

    logger.debug("----Inside getObjectsCount SOQL query: " + cadQuery);

    /* Query Sales Force for count of objects */
    final QueryResult queryResult = soapBindingStub.query(cadQuery);

    /* Iterate over records */
    if (queryResult != null) {

      /* Create one Object each record */
      final ScribeObject cADbject = new ScribeObject();

      /* Create list of elements */
      final List<Element> elementList = new ArrayList<Element>();

      /* Create inner tags <Field_Name> inside <Object> */
      elementList.add(SalesForceMessageFormatUtils.createMessageElement("Count", queryResult.getSize()));

      logger.debug("----Inside getObjectsCount objects count: " + queryResult.getSize());

      /* Add all CRM fields */
      cADbject.setXmlContent(elementList);

      /* Set the final object in command object */
      cADCommandObject.setObject(new ScribeObject[] {cADbject});
    } else {
      logger.debug("----Inside getObjectsCount no response from Sales Force");
      throw new ScribeException(ScribeResponseCodes._1005);
    }
    return cADCommandObject;
  }

  @Override
  public final ScribeCommandObject updateObject(final ScribeCommandObject cADCommandObject) throws Exception {

    /* Get Sales Force stub for the agent */
    final SoapBindingStub soapBindingStub = cRMSessionManager.getSoapBindingStub(cADCommandObject.getCrmUserId(), cADCommandObject.getCrmPassword());

    /* Create message element array */
    MessageElement[] messageElementArray = SalesForceMessageFormatUtils.createMessageElementArray(cADCommandObject.getObject()[0]);

    /* Create Sales force CRM object */
    SObject sObject = new SObject();
    sObject.setType(cADCommandObject.getObjectType());
    sObject.set_any(messageElementArray);

    /* Find start time */
    final long startTime = System.currentTimeMillis();

    /* Create SalesForce object */
    final SaveResult[] saveResults = soapBindingStub.update(new SObject[] {sObject});

    /* Find end time */
    final long endTime = System.currentTimeMillis();

    /* Calculate processing time by Sales Force */
    logger.debug("----Inside updateObject time taken in Sales Force processing: " + ((endTime - startTime)) + " msec(s)");
    if (saveResults != null) {
      for (int i = 0; i < saveResults.length; i++) {
        if (saveResults[i].isSuccess()) {
          logger.debug("---Object with id: " + saveResults[i].getId() + " is updated");

          /* Set object id in object before sending back */
          cADCommandObject.getObject()[0] =
              SalesForceMessageFormatUtils.setNodeValue("Id", saveResults[i].getId(), cADCommandObject.getObject()[0]);
        } else {
          logger.info("----Inside updateObject: error recieved from Sales Force: " + saveResults[i].getErrors()[0].getMessage());
          throw new ScribeException(ScribeResponseCodes._1001 + cADCommandObject.getObjectType() + " : Message : "
              + saveResults[i].getErrors()[0].getMessage() + " : Status Code : " + saveResults[i].getErrors()[0].getStatusCode() + " : Fields : "
              + saveResults[i].getErrors()[0].getFields());
        }
      }
    } else {
      throw new ScribeException(ScribeResponseCodes._1001 + cADCommandObject.getObjectType());
    }
    return cADCommandObject;
  }

  public final SalesForceCRMSessionManager getcRMSessionManager() {
    return cRMSessionManager;
  }

  public final void setcRMSessionManager(final SalesForceCRMSessionManager cRMSessionManager) {
    this.cRMSessionManager = cRMSessionManager;
  }

  public final String getCrmFieldsSeparator() {
    return crmFieldsSeparator;
  }

  public final void setCrmFieldsSeparator(final String crmFieldsSeparator) {
    this.crmFieldsSeparator = crmFieldsSeparator;
  }

  public final String getOrderFieldsSeparator() {
    return orderFieldsSeparator;
  }

  public final void setOrderFieldsSeparator(final String orderFieldsSeparator) {
    this.orderFieldsSeparator = orderFieldsSeparator;
  }
}
