package net.winroad.wrdoclet.builder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.winroad.wrdoclet.AbstractConfiguration;
import net.winroad.wrdoclet.data.APIParameter;
import net.winroad.wrdoclet.data.ParameterOccurs;
import net.winroad.wrdoclet.data.ParameterType;
import net.winroad.wrdoclet.data.RequestMapping;
import net.winroad.wrdoclet.data.WRDoc;
import net.winroad.wrdoclet.utils.LoggerFactory;
import net.winroad.wrdoclet.utils.UniversalNamespaceCache;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Tag;

public class DubboDocBuilder extends AbstractServiceDocBuilder {
	protected List<String> dubboInterfaces = null;

	public DubboDocBuilder(WRDoc wrDoc) {
		super(wrDoc);
		this.logger = LoggerFactory.getLogger(this.getClass());
		dubboInterfaces = this.getDubboInterfaces();
	}

	protected List<String> getDubboInterfaces() {
		List<String> result = new LinkedList<String>();
		try {
			Document dubboConfig = readXMLConfig(((AbstractConfiguration) this.wrDoc
					.getConfiguration()).dubboconfigpath);
			XPath xPath = XPathFactory.newInstance().newXPath();
			xPath.setNamespaceContext(new UniversalNamespaceCache(dubboConfig,
					false));
			NodeList serviceNodes = (NodeList) xPath.evaluate(
					"//:beans/dubbo:service", dubboConfig,
					XPathConstants.NODESET);
			for (int i = 0; i < serviceNodes.getLength(); i++) {
				Node node = serviceNodes.item(i);
				String ifc = getAttributeValue(node, "interface");
				if (ifc != null)
					result.add(ifc);
			}
		} catch (Exception e) {
			this.logger.error(e);
		}
		this.logger.debug("dubbo interface list:");
		for (String s : result) {
			this.logger.debug("interface: " + s);
		}
		return result;
	}

	@Override
	protected RequestMapping parseRequestMapping(MethodDoc methodDoc) {
		RequestMapping mapping = new RequestMapping();
		mapping.setUrl(methodDoc.toString().replaceFirst(
				methodDoc.containingClass().qualifiedName() + ".", ""));
		mapping.setTooltip(methodDoc.containingClass().simpleTypeName());
		mapping.setContainerName(methodDoc.containingClass().simpleTypeName());
		return mapping;
	}

	@Override
	protected APIParameter getOutputParam(MethodDoc methodDoc) {
		APIParameter apiParameter = null;
		if (methodDoc.returnType() != null) {
			apiParameter = new APIParameter();
			apiParameter.setParameterOccurs(ParameterOccurs.REQUIRED);
			apiParameter.setType(this.getTypeName(methodDoc.returnType(), false));
			for (Tag tag : methodDoc.tags("return")) {
				apiParameter.setDescription(tag.text());
			}
			HashSet<String> processingClasses = new HashSet<String>();
			apiParameter.setFields(this.getFields(methodDoc.returnType(),
					ParameterType.Response, processingClasses));
			apiParameter.setHistory(this.getModificationHistory(methodDoc
					.returnType()));
		}
		return apiParameter;
	}

	@Override
	protected List<APIParameter> getInputParams(MethodDoc methodDoc) {
		List<APIParameter> paramList = new LinkedList<APIParameter>();
		Parameter[] methodParameters = methodDoc.parameters();
		if (methodParameters.length != 0) {
			for (int i = 0; i < methodParameters.length; i++) {
				APIParameter p = new APIParameter();
				p.setName(methodParameters[i].name());
				p.setType(this.getTypeName(methodParameters[i].type(), false));
				p.setDescription(this.getParamComment(methodDoc, methodParameters[i].name()));
				HashSet<String> processingClasses = new HashSet<String>();
				p.setFields(this.getFields(methodParameters[i].type(),
						ParameterType.Request, processingClasses));
				paramList.add(p);
			}
		}
		return paramList;
	}

	@Override
	protected boolean isServiceInterface(ClassDoc classDoc) {
		return classDoc.isInterface()
				&& dubboInterfaces.contains(classDoc.qualifiedName());
	}

	@Override
	protected int isAPIAuthNeeded(String url) {
		//no authentication
		return -1;
	}

}
