package org.connid.bundles.unix.methods;

import java.io.IOException;
import java.util.Set;

import org.connid.bundles.unix.UnixConnection;
import org.connid.bundles.unix.UnixConnector;
import org.connid.bundles.unix.UnixResult;
import org.connid.bundles.unix.UnixResult.Operation;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

import com.jcraft.jsch.JSchException;

public class UnixCommon {
	
	public static void processPassword(UnixConnection unixConnection, String username, Set<Attribute> attrs) throws JSchException, IOException{
		final String password = Utilities.getPlainPassword(
	            AttributeUtil.getPasswordValue(attrs));
	
		if (StringUtil.isNotBlank(password)){
		UnixResult result = unixConnection.execute(
				UnixConnector.getCommandGenerator().setPassword(username, password), password);

		result.checkResult(Operation.PASSWD);
	}

	}
	
	public static void processActivation(UnixConnection unixConnection, String username, Set<Attribute> attrs) throws JSchException, IOException{
		Attribute status = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, attrs);
		if (status != null && status.getValue() != null && !status.getValue().isEmpty()){
			boolean statusValue = ((Boolean) status.getValue().get(0)).booleanValue();
			UnixResult result;
			if (!statusValue) {
				result = unixConnection.execute(UnixConnector.getCommandGenerator().lockUser(username));
				result.checkResult(Operation.USERMOD);
			} else {
				result = unixConnection.execute(UnixConnector.getCommandGenerator().unlockUser(username));
				result.checkResult(Operation.USERMOD);
			}
		}
	}
}
