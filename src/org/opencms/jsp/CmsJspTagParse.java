/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/jsp/CmsJspTagParse.java,v $
 * Date   : $Date: 2006/03/27 14:52:19 $
 * Version: $Revision: 1.2 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.jsp;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsRequestContext;
import org.opencms.flex.CmsFlexController;
import org.opencms.jsp.parse.A_CmsConfiguredHtmlParser;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.util.CmsStringUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;

import org.htmlparser.util.ParserException;

/**
 * Implements the <code>&lt;cms:parse&gt;&lt;/cms:parse&gt;</code> tag to allow parsing of nested
 * HTML with the {@link org.opencms.jsp.parse.A_CmsConfiguredHtmlParser}} implementation specified by the "parserClass" attribute.
 * <p>
 * 
 * @author Achim Westermann
 * 
 * @version $Revision: 1.2 $
 * 
 * @since 6.1.3
 */
public class CmsJspTagParse extends BodyTagSupport {

    /**
     * The name of the mandatory Tag attribute for the visitor class an instance of will be guided
     * throught the body content.
     */
    public static final String ATT_VISITOR_CLASS = "parserClass";

    /** Tag name constant for log output. */
    public static final String TAG_NAME = "parse";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspTagParse.class);

    /** Serial version UID required for safe serialization. */
    private static final long serialVersionUID = -6541745426202242240L;

    /** The visitor / parser classname to use. */
    private String m_configuredParserClassname;

    /** The attribute value of the param attribute. */
    private String m_param = "";

    /**
     * Internal action method.
     * <p>
     * 
     * Parses (and potentially transforms) a HTMl content block.
     * <p>
     * 
     * @param content the content to be parsed / transformed.
     * 
     * @param context needed for getting the encoding / the locale.
     * 
     * @param parser the visitor / parser to use.
     * 
     * @return the transformed content.
     * 
     */
    public static String parseTagAction(String content, PageContext context, A_CmsConfiguredHtmlParser parser) {

        String result = null;
        CmsRequestContext cmsContext = CmsFlexController.getCmsObject(context.getRequest()).getRequestContext();
        if (parser == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(Messages.get().getBundle(cmsContext.getLocale()).key(
                    Messages.GUI_ERR_TAG_ATTRIBUTE_MISSING_2,
                    new Object[] {TAG_NAME, ATT_VISITOR_CLASS}));
            }
            result = content;
        } else {

            String encoding = cmsContext.getEncoding();
            try {
                result = parser.doParse(content, encoding);

            } catch (ParserException pex) {

                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle(cmsContext.getLocale()).key(
                        Messages.ERR_PROCESS_TAG_1,
                        new Object[] {TAG_NAME}), pex);
                }
                StringWriter stackTrace = new StringWriter();
                PrintWriter writer = new PrintWriter(new StringWriter());
                StringBuffer msg = new StringBuffer("<!--\n").append(pex.getLocalizedMessage()).append("\n");
                pex.printStackTrace(writer);
                msg.append(stackTrace.toString()).append("\n-->");
                result = msg.toString();
            } catch (CmsException cmex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle(cmsContext.getLocale()).key(
                        Messages.ERR_PROCESS_TAG_1,
                        new Object[] {TAG_NAME}), cmex);
                }
                StringWriter stackTrace = new StringWriter();
                PrintWriter writer = new PrintWriter(new StringWriter());
                StringBuffer msg = new StringBuffer("<!--\n").append(cmex.getLocalizedMessage()).append("\n");
                cmex.printStackTrace(writer);
                msg.append(stackTrace.toString()).append("\n-->");
                result = msg.toString();
            }

        }
        return result;
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#doEndTag()
     * @return EVAL_PAGE
     * @throws JspException in case soemthing goes wrong
     */
    public int doEndTag() throws JspException {

        ServletRequest req = pageContext.getRequest();
        A_CmsConfiguredHtmlParser parser;

        // This will always be true if the page is called through OpenCms
        if (CmsFlexController.isCmsRequest(req)) {
            String content = "";
            try {
                if (CmsStringUtil.isEmpty(m_configuredParserClassname)) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(Messages.get().getBundle().key(
                            Messages.GUI_ERR_TAG_ATTRIBUTE_MISSING_2,
                            new Object[] {TAG_NAME, ATT_VISITOR_CLASS}));
                    }

                }
                // wrong attribute visitorClass -> content will remain empty, but no exception is
                // thrown
                try {
                    // load
                    Class cl = Class.forName(m_configuredParserClassname);
                    // instanciate
                    Object instance = cl.newInstance();
                    // cast
                    parser = (A_CmsConfiguredHtmlParser)instance;
                    parser.setParam(m_param);
                    // cms object:
                    CmsFlexController controller = CmsFlexController.getController(req);
                    CmsObject cms = controller.getCmsObject();
                    parser.setCmsObject(cms);
                    content = parseTagAction(getBodyContent().getString(), pageContext, parser);

                } catch (Exception e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(Messages.get().getBundle().key(
                            Messages.GUI_ERR_TAG_ATTRIBUTE_INVALID_3,
                            new Object[] {TAG_NAME, ATT_VISITOR_CLASS, A_CmsConfiguredHtmlParser.class.getName()}), e);
                    }
                    e.printStackTrace(System.err);
                }

            } finally {
                try {
                    getBodyContent().clear();
                    getBodyContent().print(content);
                    getBodyContent().writeOut(pageContext.getOut());
                    // need to release manually, JSP container may not call release as required
                    // (happens with Tomcat)
                    release();

                } catch (Exception ex) {
                    release();
                    if (LOG.isErrorEnabled()) {
                        LOG.error(Messages.get().getBundle().key(Messages.ERR_PROCESS_TAG_1, TAG_NAME), ex);
                    }
                    // this is severe
                    throw new JspException(ex);
                }

            }

        }
        return EVAL_PAGE;
    }

    /**
     * Returns the param.
     * <p>
     * 
     * @return the param
     */
    public String getParam() {

        return m_param;
    }

    /**
     * Returns the fully qualified class name of the {@link A_CmsConfiguredHtmlParser} class to use
     * for parsing.
     * <p>
     * 
     * @return the parserrClass
     */
    public String getParserClass() {

        return m_configuredParserClassname;
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    public void release() {

        m_configuredParserClassname = null;
        m_param = null;
        super.release();
    }

    /**
     * Sets the param.
     * <p>
     * 
     * @param param the param to set
     */
    public void setParam(String param) {

        m_param = param;
    }

    /**
     * Sets the fully qualified class name of the {@link A_CmsConfiguredHtmlParser} class to use for
     * parsing.
     * <p>
     * 
     * @param parserClass the fully qualified class name of the {@link A_CmsConfiguredHtmlParser}
     *            class to use for parsing.
     */
    public void setParserClass(String parserClass) {

        m_configuredParserClassname = parserClass;
    }

}
