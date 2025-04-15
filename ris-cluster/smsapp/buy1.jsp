<% /*
    * Redirect page for accesscode registration.  The documented URL for registration is
    * /register/reg1.jsp.  This page intercepts those requests and redirects into the     
    * appropriate
    * URI to handle access code registration requests.  All request name/value pairs are
    * preserved.
    */
%>
<%--      testing.......--%>

<%@ page
    import="java.net.URLEncoder"
    errorPage="/reg/include/errPage.jsp"
%>

<%
  // where we're gonna send them
  String redirectTarget = "/reg/buy/buy1.jsp";

  // preserve request params
  String queryParams = request.getQueryString();
  String newURL = "";
  if(queryParams != null)
  {
    // tack query params onto the redirect url and
    newURL = redirectTarget + "?" + queryParams;
  }
  else
  {
    newURL = redirectTarget;
  }

  // send them along
  response.sendRedirect(response.encodeURL(newURL));

%>
