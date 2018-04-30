package org.esupportail.sgc.services.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.esupportail.sgc.domain.User;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class SpelGroupService implements GroupService {
	
	private Map<String, String> groups4eppnSpel = new HashMap<String, String>();

	public void setGroups4eppnSpel(Map<String, String> groups4eppnSpel) {
		this.groups4eppnSpel = groups4eppnSpel;
	}


	@Override
	public List<String> getGroupsForEppn(String eppn) {
		
		List<String> groups = new ArrayList<String>();
		
		User user = User.findUser(eppn);
		
		if(user!=null) {
			for(String groupName: groups4eppnSpel.keySet()) {
				String expression = groups4eppnSpel.get(groupName);
				ExpressionParser parser = new SpelExpressionParser();
				Expression exp = parser.parseExpression(expression);
	
				EvaluationContext context = new StandardEvaluationContext();
				context.setVariable("user", user);
				
				Boolean value = (Boolean) exp.getValue(context);
				if(value) {
					groups.add(groupName);
				}
			}
		}
		
		return groups;
		
	}

	@Override
	public List<String> getMembers(String groupName) {

		Set<String> members = new HashSet<String>();

		for(String eppn : User.findAllEppns()) {
			if(getGroupsForEppn(eppn).contains(groupName)) {
				members.add(eppn);
			}
		}
		
		return new ArrayList<String>(members);
		
	}
	
}

