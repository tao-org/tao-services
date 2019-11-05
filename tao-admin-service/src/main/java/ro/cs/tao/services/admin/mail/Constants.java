/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package ro.cs.tao.services.admin.mail;

public class Constants {
    public static final String MAIL_CONTENTS = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "<head>\n" +
            "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
            "\t<title>Account activation</title>\n" +
            "<style type=\"text/css\">\n" +
            "body{width:100% !important;}\n" +
            "body{margin:0;padding:0;font-family: \"Helvetica Neue\", \"Helvetica\", Helvetica, Arial, sans-serif;font-size:12px;color:#333333;background-color: #FFFFFF;}\n" +
            "img{border:0;line-height:100%;outline:none;text-decoration:none;}\n" +
            "table td{border-collapse:collapse;}\n" +
            "a:link, a:visited {color:#336699;}\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" height=\"100%\" width=\"100%\" id=\"bodyTable\" bgcolor=\"#07123a\">\n" +
            "    <tr>\n" +
            "        <td align=\"center\" valign=\"top\">\n" +
            "            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" id=\"emailContainer\">\n" +
            "                <tr>\n" +
            "                    <td align=\"center\" valign=\"top\">\n" +
            "                        <table border=\"0\" cellpadding=\"20\" cellspacing=\"0\" width=\"100%\" id=\"emailHeader\">\n" +
            "                            <tr>\n" +
            "\t\t\t\t\t\t\t\t<td align=\"center\" valign=\"top\" style=\"border-collapse:collapse;color:#ffffff;font-size:14px;line-height:150%;text-align:center;\">\n" +
            "TAO Platform - Account activation\n" +
            "                                </td>\n" +
            "                            </tr>\n" +
            "                        </table>\n" +
            "                    </td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td align=\"center\" valign=\"top\">\n" +
            "                        <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" id=\"emailBody\" bgcolor=\"#ffffff\">\n" +
            "                            <tr>\n" +
            "                                <td align=\"left\" valign=\"top\" style=\"padding:10px;\">\n" +
            "<p>\n" +
            "To: $USERNAME\n" +
            "<br><br>Hello! <br>You received this email as a consequence of your account creation. <br>For activating you account click on the following activation link: </br><strong><a href=\"$LINK\">Activate your account</a></strong>. \n" +
            "</p>\n" +
            "\n" +
            "                                </td>\n" +
            "                            </tr>\n" +
            "                        </table>\n" +
            "                    </td>\n" +
            "                </tr>\n" +
            "                <tr>\n" +
            "                    <td align=\"center\" valign=\"top\">\n" +
            "                        <table border=\"0\" cellpadding=\"20\" cellspacing=\"0\" width=\"100%\" id=\"emailFooter\">\n" +
            "                            <tr>\n" +
            "                                <td align=\"center\" valign=\"top\" style=\"border-collapse:collapse;color:#ffffff;font-size:12px;line-height:150%;text-align:center;\">\n" +
            "<em>TAO Administration</em>\n" +
            "                                </td>\n" +
            "                            </tr>\n" +
            "                        </table>\n" +
            "                    </td>\n" +
            "                </tr>\n" +
            "            </table>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "</body>\n" +
            "</html>";
}
