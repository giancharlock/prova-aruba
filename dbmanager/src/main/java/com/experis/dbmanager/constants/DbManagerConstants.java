package com.experis.dbmanager.constants;

public final class DbManagerConstants {

    private DbManagerConstants() {
        // restrict instantiation
    }

    public static final String  DBMANAGER = "DbManager";
    public static final int  NEW_DBMANAGER_LIMIT = 1_000_000;
    public static final String  STATUS_201 = "201";
    public static final String  MESSAGE_201 = "Item created successfully";
    public static final String  STATUS_200 = "200";
    public static final String  MESSAGE_200 = "Request processed successfully";
    public static final String  STATUS_417 = "417";
    public static final String  MESSAGE_417_UPDATE= "Update operation failed. Please try again or contact Dev team";
    public static final String  MESSAGE_417_DELETE= "Delete operation failed. Please try again or contact Dev team";
    // public static final String  STATUS_500 = "500";
    // public static final String  MESSAGE_500 = "An error occurred. Please try again or contact Dev team";

}
