(function () {
  "use strict";

  var hmacState = {
    enabled: false,
    secret: ""
  };

  function bytesToHex(bytes) {
    return Array.prototype.map.call(bytes, function (byte) {
      return byte.toString(16).padStart(2, "0");
    }).join("");
  }

  async function getBodyBytes(body) {
    var encoder = new TextEncoder();
    if (body == null) {
      return encoder.encode("");
    }
    if (typeof body === "string") {
      return encoder.encode(body);
    }
    if (body instanceof ArrayBuffer) {
      return new Uint8Array(body);
    }
    if (ArrayBuffer.isView(body)) {
      return new Uint8Array(body.buffer, body.byteOffset, body.byteLength);
    }
    if (body instanceof URLSearchParams) {
      return encoder.encode(body.toString());
    }
    if (body instanceof Blob) {
      return new Uint8Array(await body.arrayBuffer());
    }
    return encoder.encode(JSON.stringify(body));
  }

  async function sha256Hex(bytes) {
    var hash = await crypto.subtle.digest("SHA-256", bytes);
    return bytesToHex(new Uint8Array(hash));
  }

  async function hmacSha256Hex(secret, data) {
    var encoder = new TextEncoder();
    var key = await crypto.subtle.importKey(
      "raw",
      encoder.encode(secret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );
    var signature = await crypto.subtle.sign(
      "HMAC",
      key,
      encoder.encode(data)
    );
    return bytesToHex(new Uint8Array(signature));
  }

  function createNonce() {
    if (crypto.randomUUID) {
      return crypto.randomUUID();
    }
    var bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    return bytesToHex(bytes);
  }

  function setHeader(headers, name, value) {
    if (headers instanceof Headers) {
      headers.set(name, value);
      return headers;
    }
    var nextHeaders = headers || {};
    nextHeaders[name] = value;
    return nextHeaders;
  }

  function shouldSignRequest(url) {
    var parsed = new URL(url, window.location.origin);
    return parsed.origin === window.location.origin
      && parsed.pathname.startsWith("/api/");
  }

  async function signRequest(request) {
    if (!hmacState.enabled || !hmacState.secret || !shouldSignRequest(request.url)) {
      return request;
    }

    var parsed = new URL(request.url, window.location.origin);
    var pathWithQuery = parsed.pathname + parsed.search;
    var timestamp = String(Date.now());
    var nonce = createNonce();
    var bodyHash = await sha256Hex(await getBodyBytes(request.body));
    var method = (request.method || "GET").toUpperCase();
    var canonical = [
      method,
      pathWithQuery,
      timestamp,
      nonce,
      bodyHash,
      ""
    ].join("\n");
    var signature = await hmacSha256Hex(hmacState.secret, canonical);

    request.headers = setHeader(request.headers, "X-Hmac-Signature", signature);
    request.headers = setHeader(request.headers, "X-Hmac-Timestamp", timestamp);
    request.headers = setHeader(request.headers, "X-Hmac-Nonce", nonce);
    return request;
  }

  function renderToolbar() {
    var toolbar = document.getElementById("schemafy-hmac-toolbar");
    if (!toolbar) {
      return;
    }

    toolbar.innerHTML = [
      "<strong>Dev HMAC</strong>",
      "<label><input id=\"schemafy-hmac-enabled\" type=\"checkbox\">Sign requests</label>",
      "<input id=\"schemafy-hmac-secret\" type=\"password\" autocomplete=\"off\" spellcheck=\"false\" placeholder=\"HMAC secret\">",
      "<button id=\"schemafy-hmac-clear\" type=\"button\">Clear</button>",
      "<span class=\"schemafy-hmac-status\" id=\"schemafy-hmac-status\">Secret is kept in this page only.</span>"
    ].join("");

    var enabledInput = document.getElementById("schemafy-hmac-enabled");
    var secretInput = document.getElementById("schemafy-hmac-secret");
    var clearButton = document.getElementById("schemafy-hmac-clear");
    var status = document.getElementById("schemafy-hmac-status");

    enabledInput.addEventListener("change", function () {
      hmacState.enabled = enabledInput.checked;
      status.textContent = hmacState.enabled
        ? "Signing /api requests before send."
        : "HMAC signing is off.";
    });

    secretInput.addEventListener("input", function () {
      hmacState.secret = secretInput.value;
    });

    clearButton.addEventListener("click", function () {
      hmacState.secret = "";
      secretInput.value = "";
      secretInput.focus();
      status.textContent = "Secret cleared.";
    });
  }

  window.onload = function () {
    renderToolbar();
    window.ui = SwaggerUIBundle({
      url: "/openapi/openapi3.json",
      dom_id: "#swagger-ui",
      deepLinking: true,
      requestInterceptor: signRequest,
      presets: [
        SwaggerUIBundle.presets.apis,
        SwaggerUIStandalonePreset
      ],
      plugins: [
        SwaggerUIBundle.plugins.DownloadUrl
      ],
      layout: "StandaloneLayout"
    });
  };
})();
