package com.bc.wps.api;

import com.bc.wps.api.exceptions.WpsServiceException;
import com.bc.wps.api.schema.Capabilities;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.ProcessDescriptionType;
import java.util.List;

public interface WpsServiceInstance {
   Capabilities getCapabilities(WpsRequestContext var1) throws WpsServiceException;

   List<ProcessDescriptionType> describeProcess(WpsRequestContext var1, String var2) throws WpsServiceException;

   ExecuteResponse doExecute(WpsRequestContext var1, Execute var2) throws WpsServiceException;

   ExecuteResponse getStatus(WpsRequestContext var1, String var2) throws WpsServiceException;

   void dispose();
}
