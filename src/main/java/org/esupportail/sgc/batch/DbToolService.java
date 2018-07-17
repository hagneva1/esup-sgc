package org.esupportail.sgc.batch;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.esupportail.sgc.domain.AppliConfig;
import org.esupportail.sgc.domain.AppliConfig.TypeConfig;
import org.esupportail.sgc.domain.AppliVersion;
import org.esupportail.sgc.domain.BigFile;
import org.esupportail.sgc.domain.CardActionMessage;
import org.esupportail.sgc.domain.PhotoFile;
import org.esupportail.sgc.domain.User;
import org.esupportail.sgc.services.userinfos.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class DbToolService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	final static String currentEsupSgcVersion = "0.2.u";
		
	@Resource
	DataSource dataSource;
	
	@Resource
	UserInfoService userInfoService;

	@Transactional
	public void upgrade() {
		AppliVersion appliVersion = null;
		List<AppliVersion> appliVersions = AppliVersion.findAllAppliVersions("esupSgcVersion", "desc");
		if(appliVersions.isEmpty()) {
			appliVersion = new AppliVersion();
			appliVersion.setEsupSgcVersion("0.0.x");
			appliVersion.persist();
		} else {
			appliVersion = appliVersions.get(0);
			for(int i = 1; i<appliVersions.size() ; i++) {
				appliVersions.get(i).remove();
			}
		}
		upgradeIfNeeded(appliVersion);
	}

	private void upgradeIfNeeded(AppliVersion appliVersion) {
		String esupSgcVersion = appliVersion.getEsupSgcVersion();
		try{
			if("0.0.x".equals(esupSgcVersion)) {
				String sqlUpdate = "insert into card_desfire_ids (card, desfire_ids, desfire_ids_key) select id, desfire_id, 'access-control' from card where desfire_id <> '';";
				sqlUpdate += "insert into card_desfire_ids (card, desfire_ids, desfire_ids_key) select card.id, crous_smart_card.id_zdc, 'crous' from card, crous_smart_card where card.csn = crous_smart_card.uid;";
				log.warn("La commande SQL suivante va être exécutée : \n" + sqlUpdate);
				Connection connection = dataSource.getConnection();
				CallableStatement statement = connection.prepareCall(sqlUpdate);
				statement.execute();
				connection.close();
				
				sqlUpdate = "ALTER TABLE card DROP COLUMN IF EXISTS card_id_generator;";
				sqlUpdate += "ALTER TABLE card DROP COLUMN IF EXISTS desfire_id;";
				sqlUpdate += "ALTER TABLE crous_smart_card DROP COLUMN IF EXISTS crous_smart_card_id_generator;";	
				sqlUpdate += "DROP TABLE crous_smart_card_id_generator, card_id_generator;";
				log.warn("La commande SQL suivante va être exécutée : \n" + sqlUpdate);
				connection = dataSource.getConnection();
				statement = connection.prepareCall(sqlUpdate);
				statement.execute();
				connection.close();
				
				log.warn("\n\n#####\n\t" +
	    				"Pensez à mettre à jour les configurations de l'application depuis l'IHM !" +
	    				"\n#####\n");
	    		
	    		esupSgcVersion = "0.1.x";
	    		
			} 
			if("0.1.x".equals(esupSgcVersion)) {
				
				/*
				for(User user: User.findAllUsers()) {
					userInfoService.setAdditionalsInfo(user, null);
					log.info("update userinfos of " + user.getEppn() + " : template is now " + user.getTemplateKey());
					for(Card card : user.getCards()) {
						if(Etat.PRINTED.equals(card.getEtat()) || Etat.ENCODED.equals(card.getEtat()) || Etat.ENABLED.equals(card.getEtat()) || Etat.DISABLED.equals(card.getEtat()) || Etat.CADUC.equals(card.getEtat())) {
							card.setTemplateCard(user.getTemplateCard());
						}
					}
				}
				*/
				// trop long : plus simple/efficace de passer leq requêtes à la main. 
				// update user_account set template_key = 'univ-rouen' where eppn like '%@univ-rouen.fr';
				// update card set template_card = (select id from template_card where key = 'univ-rouen') where eppn like '%@univ-rouen.fr' and etat in ('ENCODED', 'ENABLED', 'PRINTED', 'DISABLED', 'CADUC');
				//
				
	    		esupSgcVersion = "0.1.y";
	    		
			}
			if("0.1.y".equals(esupSgcVersion)) {
				
				String sqlUpdate = "alter table user_account drop column if exists last_card_template;";
				
				sqlUpdate += "alter table user_account disable trigger tsvectorupdateuser;";
				sqlUpdate += "with cards as (select distinct on(request_date) template_card, eppn, request_date from card order by request_date desc) update user_account set last_card_template_printed = cards.template_card from cards where cards.eppn = user_account.eppn;";
				sqlUpdate += "alter table user_account enable trigger tsvectorupdateuser;";
				
				// hack
				sqlUpdate += "update appli_version set version = 0 where version is null;";
				
				log.warn("La commande SQL suivante va être exécutée : \n" + sqlUpdate);
				Connection connection = dataSource.getConnection();
				CallableStatement statement = connection.prepareCall(sqlUpdate);
				statement.execute();
				connection.close();
				
				if(AppliConfig.findAppliConfigsByKeyEquals("PHOTO_SIZE_MAX").getResultList().isEmpty()) {
					AppliConfig appliConfig = new AppliConfig();
					appliConfig.setKey("PHOTO_SIZE_MAX");
					appliConfig.setDescription("Taille maximale (en octets) de la photo que l''on peut télécharger lors de la demande de carte");
					appliConfig.setValue("200000");
					appliConfig.setType(TypeConfig.TEXT);
					appliConfig.persist();
				}
							
	    		esupSgcVersion = "0.1.z";
			}
			if("0.1.z".equals(esupSgcVersion)) {
				
				// Fixe : 2 photo_files qui pointent sur le même big_file (issu d'un renouvellement) 
				String sqlSelectBigFilePb = "select a.id as aid, b.id as bid from photo_file a, photo_file b where a.big_file=b.big_file and a.id<>b.id";
				Connection connection = dataSource.getConnection();
				CallableStatement statement = connection.prepareCall(sqlSelectBigFilePb);
				statement.execute();
				ResultSet photoFilesIdRs = statement.getResultSet();			
				while(photoFilesIdRs.next()) {
					int aid = photoFilesIdRs.getInt("aid");
					int bid = photoFilesIdRs.getInt("bid");
					int id = Math.max(aid, bid);
					int id4Copy = Math.min(aid, bid);
					PhotoFile photoFile4Copy = PhotoFile.findPhotoFile(Long.valueOf(id4Copy));
					PhotoFile photoFile = PhotoFile.findPhotoFile(Long.valueOf(id));
					photoFile.setBigFile(new BigFile());
					photoFile.getBigFile().setBinaryFile(photoFile4Copy.getBigFile().getBinaryFileasBytes());
					photoFile.setFileSize(photoFile4Copy.getFileSize());
					photoFile.merge();
				}				
				connection.close();
				
				Set<String> userTypes = new HashSet<String>(User.findDistinctUserType());
				for(CardActionMessage message : CardActionMessage.findAllCardActionMessages()) {
					message.setUserTypes(userTypes);
					message.merge();
				}
				
	    		esupSgcVersion = "0.2.u";
			}
			else {
				log.warn("\n\n#####\n\t" +
	    				"Base de données à jour !" +
	    				"\n#####\n");
			}
			appliVersion.setEsupSgcVersion(currentEsupSgcVersion);
			appliVersion.merge();
		} catch(Exception e) {
			throw new RuntimeException("Erreur durant le mise à jour de la base de données", e);
		}
	}

}
