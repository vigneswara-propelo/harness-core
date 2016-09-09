package software.wings.filter;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.HttpMethod;
import software.wings.common.AuditHelper;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.BoundedInputStream;

import java.io.IOException;
import java.net.InetAddress;
import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 * AuditRequestFilter preserves the rest endpoint header and payload.
 *
 * @author Rishi
 */
@Singleton
@Provider
@Priority(1)
public class AuditRequestFilter implements ContainerRequestFilter {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Context private ResourceContext resourceContext;

  @Inject private AuditHelper auditHelper;
  @Inject private FileService fileService;
  @Inject private MainConfiguration configuration;

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    AuditHeader header = new AuditHeader();
    String url = requestContext.getUriInfo().getAbsolutePath().toString();
    header.setUrl(url);

    String headerString = getHeaderString(requestContext.getHeaders());
    header.setHeaderString(headerString);

    String query = getQueryParams(requestContext.getUriInfo().getQueryParameters());
    header.setQueryParams(query);

    HttpMethod method = HttpMethod.valueOf(requestContext.getMethod());
    header.setRequestMethod(method);
    header.setResourcePath(requestContext.getUriInfo().getPath());
    header.setRequestTime(System.currentTimeMillis());

    HttpServletRequest request = resourceContext.getResource(HttpServletRequest.class);

    header.setRemoteHostName(request.getRemoteHost());
    header.setRemoteIpAddress(request.getRemoteAddr());
    header.setRemoteHostPort(request.getRemotePort());
    header.setLocalHostName(InetAddress.getLocalHost().getHostName());
    header.setLocalIpAddress(InetAddress.getLocalHost().getHostAddress());

    header = auditHelper.create(header);

    try {
      BoundedInputStream inputStream = new BoundedInputStream(
          requestContext.getEntityStream(), configuration.getFileUploadLimits().getAppContainerLimit());
      String fileId = auditHelper.create(header, RequestType.REQUEST, inputStream);
      requestContext.setEntityStream(fileService.openDownloadStream(fileId, FileBucket.AUDITS));
    } catch (Exception exception) {
      throw new WingsException(exception);
    }
  }

  private String getHeaderString(MultivaluedMap<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return "";
    } else {
      StringBuilder headerString = new StringBuilder();
      for (String key : headers.keySet()) {
        headerString.append(key).append("=");
        for (String value : headers.get(key)) {
          headerString.append(";");
          headerString.append(key.equalsIgnoreCase("Authorization") ? "********" : value);
        }
        headerString.substring(1);
        headerString.append(",");
      }
      String headerStr = headerString.toString();
      if (headerStr.length() > 0) {
        headerStr = headerStr.substring(0, headerStr.length() - 1);
      }
      return headerStr;
    }
  }

  private String getQueryParams(MultivaluedMap<String, String> queryParameters) {
    String queryParams = "";
    for (String key : queryParameters.keySet()) {
      String temp = "";
      for (String value : queryParameters.get(key)) {
        temp += "&" + key + "=" + value;
      }
      queryParams += "&" + temp.substring(1);
    }
    if (queryParams.equals("")) {
      return null;
    } else {
      return queryParams.substring(1);
    }
  }
}
