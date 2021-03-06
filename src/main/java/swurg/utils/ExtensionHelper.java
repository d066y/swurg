/*
#    Copyright (C) 2016 Alexandre Teyar

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
#    limitations under the License. 
*/

package swurg.utils;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExtensionHelper {

  private IExtensionHelpers burpExtensionHelpers;

  public ExtensionHelper(IBurpExtenderCallbacks callbacks) {
    this.burpExtensionHelpers = callbacks.getHelpers();
  }

  public IExtensionHelpers getBurpExtensionHelpers() {
    return burpExtensionHelpers;
  }

  public int getPort(
      Swagger swagger, Scheme scheme
  ) {
    int port;

    if (swagger.getHost().split(":").length > 1) {
      port = Integer.valueOf(swagger.getHost().split(":")[1]);
    } else {
      if (scheme.toValue().toUpperCase().equals("HTTPS")) {
        port = 443;
      } else {
        port = 80;
      }
    }

    return port;
  }

  public boolean isUseHttps(Scheme scheme) {
    boolean useHttps;

    useHttps = scheme.toValue().toUpperCase().equals("HTTPS") || scheme.toValue().toUpperCase()
        .equals("WSS");

    return useHttps;
  }

  private List<String> buildHeaders(
      Swagger swagger, Map.Entry<String, Path> path, Map.Entry<HttpMethod, Operation> operation
  ) {
    List<String> headers = new ArrayList<>();

    headers.add(
        operation.getKey().toString() + " " + swagger.getBasePath() + path.getKey() + " HTTP/1.1");
    headers.add("Host: " + swagger.getHost().split(":")[0]);

    if (operation.getValue().getProduces() != null && !operation.getValue().getProduces()
        .isEmpty()) {
      headers.add("Accept: " + String.join(",", operation.getValue().getProduces()));
    } else if (swagger.getProduces() != null && !swagger.getProduces().isEmpty()) {
      headers.add("Accept: " + String.join(",", swagger.getProduces()));
    }

    if (operation.getValue().getConsumes() != null && !operation.getValue().getConsumes()
        .isEmpty()) {
      headers.add("Content-Type: " + String.join(",", operation.getValue().getConsumes()));
    } else if (swagger.getConsumes() != null && !swagger.getConsumes().isEmpty()) {
      headers.add("Content-Type: " + String.join(",", swagger.getConsumes()));
    }

    return headers;
  }

  public byte[] buildRequest(
      Swagger swagger, Map.Entry<String, Path> path, Map.Entry<HttpMethod, Operation> operation
  ) {
    List<String> headers = buildHeaders(swagger, path, operation);
    byte[] httpMessage = this.burpExtensionHelpers.buildHttpMessage(headers, null);

    for (Parameter parameter : operation.getValue().getParameters()) {
      if (parameter.getIn().equals("query")) {
        httpMessage = this.burpExtensionHelpers.addParameter(httpMessage, this.burpExtensionHelpers
            .buildParameter(parameter.getName(), "fuzzMe", (byte) 0));
      } else if (parameter.getIn().equals("body")) {
        httpMessage = this.burpExtensionHelpers.addParameter(httpMessage, this.burpExtensionHelpers
            .buildParameter(parameter.getName(), "fuzzMe", (byte) 1));
      }
    }

    return httpMessage;
  }
}
