<%--@elvariable id="currentUser" type="pl.edu.pw.ee.pyskp.documentworkflow.dto.UserInfoDTO"--%>
<%--@elvariable id="project" type="pl.edu.pw.ee.pyskp.documentworkflow.dto.ProjectInfoDTO"--%>
<%--@elvariable id="_csrf" type="org.springframework.security.web.csrf.DefaultCsrfToken"--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<html>
<head>
    <%@ include file="css.jsp" %>
    <title>System obiegu dokumentów - stwórz zadanie</title>
</head>
<body>

<div class="page-header">
    <h1>
        <img src="<spring:url value="/images/logo.png"/>" width="40px" height="40px">
        <a href="<spring:url value="/projects/${project.id}"/>">
            ${project.name}
        </a>
        <small>Dodawanie nowego zadania</small>
    </h1>
</div>

<nav class="navbar navbar-inverse">
    <div class="container-fluid">
        <ul class="nav navbar-nav">
            <li>
                <a href="<spring:url value="/"/>">
                    <span class="glyphicon glyphicon-home"></span> Strona główna
                </a>
            </li>
            <li class="active">
                <a href="<spring:url value="/projects"/>">
                    <span class="glyphicon glyphicon-folder-close"></span> Projekty
                </a>
            </li>
            <li>
                <a href="<spring:url value="/tasks"/>">
                    <span class="glyphicon glyphicon-tasks"></span> Zadania
                </a>
            </li>
        </ul>

        <p class="navbar-text">Zalogowany jako ${currentUser.fullName}</p>

        <form class="navbar-form navbar-right" action="<c:url value="/logout" />" method="post">
            <button id="logout" type="submit" class="btn btn-default">
                <span class="glyphicon glyphicon-log-out"></span> Wyloguj
            </button>
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        </form>
    </div>
</nav>

<div class="container-fluid">
    <form:form modelAttribute="newTaskForm" class="form-horizontal">
        <fieldset>
            <legend>
                <spring:message code="addTask.form.legend"/>
            </legend>

            <div class="form-group">
                <label class="control-label col-md-2" for="name">
                    <spring:message code="addTask.form.name.label"/>
                </label>
                <div class="col-md-5">
                    <form:input path="name" id="name" type="text" class="form-control"/>
                    <p><form:errors path="name" cssClass="text-danger"/></p>
                </div>
            </div>

            <div class="form-group">
                <label class="control-label col-md-2" for="description">
                    <spring:message code="addTask.form.description.label"/>
                </label>
                <div class="col-md-5">
                    <form:textarea path="description" id="description" rows="4" cssClass="form-control"/>
                    <p><form:errors path="description" cssClass="text-danger"/></p>
                </div>
            </div>

            <div class="form-group">
                <label class="control-label col-md-2" for="administratorEmail">
                    <spring:message code="addTask.form.administrator.label"/>
                </label>
                <div class="col-md-5">
                    <form:input path="administratorEmail" id="administratorEmail" cssClass="form-control"/>
                    <p><form:errors path="administratorEmail" cssClass="text-danger"/></p>
                </div>
            </div>

            <div class="form-group">
                <div class="col-lg-offset-2 col-lg-10">
                    <input type="submit" id="btnAdd" class="btn btn-primary" value="Stwórz"/>
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                </div>
            </div>
        </fieldset>
    </form:form>
</div>

</body>
</html>