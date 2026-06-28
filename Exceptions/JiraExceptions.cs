namespace AuditHub.Exceptions;

public class JiraTokenMissingException(string message) : Exception(message);
public class JiraAuthException(string message) : Exception(message);
public class JiraForbiddenException(string message) : Exception(message);
public class JiraInvalidQueryException(string message) : Exception(message);
public class JiraConnectionException(string message) : Exception(message);
