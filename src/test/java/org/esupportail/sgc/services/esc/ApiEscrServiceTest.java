package org.esupportail.sgc.services.esc;

import java.util.List;

import javax.annotation.Resource;

import org.esupportail.sgc.domain.EscrStudent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/spring/applicationContext*.xml"})
public class ApiEscrServiceTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	ApiEscrService apiEscrService;
	
	@Test
	public void getEscrStudentTest() {
		List<EscrStudent> escrStudents = EscrStudent.findAllEscrStudents();
		if(!escrStudents.isEmpty()) {
			EscrStudent escrStudentFromDb = escrStudents.get(0);
			log.info("escrStudentFromDb : " + escrStudentFromDb);
			EscrStudent escrStudentFromEscr = apiEscrService.getEscrStudent(escrStudentFromDb.getEppn());
			log.info("escrStudentFromEscr : " + escrStudentFromEscr);
			
		}
	}

}

