package org.esupportail.sgc.domain;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PostPersist;
import javax.persistence.Query;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.sgc.web.manager.CardSearchBean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.transaction.annotation.Transactional;

@RooJavaBean
@RooToString
@RooDbManaged(automaticallyDelete = true)
@RooJpaActiveRecord(versionField = "", table = "Card", finders = { "findCardsByEppnEquals", "findCardsByEppnAndEtatEquals", "findCardsByEppnLike", "findCardsByEtatEqualsAndDateDemandeLessThan", "findCardsByDesfireId", "findCardsByCsn", "findCardsByEppnAndEtatNotEquals" })
public class Card {

    public static final List<String> fieldNames4OrderClauseFilter = java.util.Arrays.asList("id", "eppn", "crous", "etat", "dateEtat", "commentaire", "flagAdresse", "adresse", "structure", "requestDate", "nbRejets", "lastEncodedDate");

    public static enum FlagAdresse {
        INT, EXT
    };

    public static enum Etat {
        NEW, REQUEST_CHECKED, IN_PRINT, PRINTED, IN_ENCODE, ENCODED, ENABLED, REJECTED, DISABLED, CADUC
    };

    public static enum MotifDisable {
        LOST, THEFT, OUT_OF_ORDER
    };

    @Column
    private String eppn;

    @ElementCollection(fetch=FetchType.LAZY)
    @Column
    private Map<String, String> desfireIds = new HashMap<String, String>();

    @Column(unique=true,nullable=true)
    private String csn = null;

    private String recto1Printed = "";

    private String recto2Printed = "";

    private String recto3Printed = "";

    private String recto4Printed = "";

    private String recto5Printed = "";

    private String versoTextPrinted;

    @ManyToOne
    private User userAccount;

    @Column
    @Enumerated(EnumType.STRING)
    private Etat etat = Etat.NEW;

    @Column
    private String etatEppn;

    @Column
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date requestDate = new Date();

    @Column
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date dateEtat = new Date();

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column
    @Enumerated(EnumType.STRING)
    private FlagAdresse flagAdresse;

    @Column
    private String addressRequested;

    @Column
    private String structure;

    @Column
    private String payCmdNum;

    @Column
    @Enumerated(EnumType.STRING)
    private MotifDisable motifDisable;

    @Column
    private String requestBrowser;

    @Column
    private String requestOs;

    @Column
    private Date deliveredDate = null;

    @Column
    private Date encodedDate = null;

    @Column
    private Date lastEncodedDate = null;

    @Column
    private Date dueDate;

    @Column
    private Date ennabledDate;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private PhotoFile photoFile = new PhotoFile();

    @Column
    private Long nbRejets;
    
    /**
     * European Student Card Number 
     */
    @Column
    private String escnUid;
    
    @Column
    private String qrcode;
    
    @Column
    private Boolean external = false;

    @Transient
    List<Etat> etatsAvailable = new ArrayList<Etat>();

    @Transient
    Boolean crousTransient;
    
    @Transient
    Boolean europeanTransient;

    @Transient
    Boolean difPhotoTransient;

    @Transient
    Boolean isPhotoEditable;
    

    @PostPersist
    public void updateNbCards() {
        int nbCards = 1 + this.getUser().getCards().size();
        this.getUser().setNbCards((long) nbCards);
    }

    public Long getNbRejets() {
        return nbRejets;
    }

    public void setNbRejets(Long nbRejets) {
        this.nbRejets = nbRejets;
    }

    public Boolean getIsPhotoEditable() {
        return isPhotoEditable;
    }

    public void setIsPhotoEditable(Boolean isPhotoEditable) {
        this.isPhotoEditable = isPhotoEditable;
    }

    public Boolean getUserEditable() {
        return getUser().isEditable();
    }

    public String getDisplayName() {
        return getUser().getDisplayName();
    }

    public String getSupannEmpId() {
        return getUser().getSupannEmpId();
    }

    public String getSupannEtuId() {
        return getUser().getSupannEtuId();
    }

    public User getUser() {
        return getUserAccount();
    }

    public String getUserType() {
        return getUser().getUserType();
    }

    public Long getNbCards() {
        return getUser().getNbCards();
    }

    public Boolean getCrous() {
        return getUserAccount().getCrous();
    }

    public void setCrous(Boolean crous) {
        getUserAccount().setCrous(crous);
    }

    public Boolean getDifPhoto() {
        return getUserAccount().getDifPhoto();
    }

    public void setDifPhoto(Boolean cnil) {
        getUserAccount().setDifPhoto(cnil);
    }

    public String getAddress() {
        return getUserAccount().getAddress();
    }

    public static Card findCard(String csn) {
        List<Card> cards = Card.findCardsByCsn(csn).getResultList();
        if (cards.isEmpty()) {
            return null;
        } else {
            return cards.get(0);
        }
    }

    public static TypedQuery<Card> findCardsByEtatIn(List<Etat> etats) {
        EntityManager em = Card.entityManager();
        TypedQuery<Card> q = em.createQuery("SELECT o FROM Card AS o WHERE o.etat IN (:etats) order by dateEtat desc", Card.class);
        q.setParameter("etats", etats);
        return q;
    }

    public static TypedQuery<Card> findCardsByEppnInAndEtatIn(List<String> eppns, List<Etat> etats, String sortFieldName, String sortOrder) {
        EntityManager em = Card.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Card AS o WHERE o.eppn IN (:eppns) AND o.etat IN (:etats)");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Card> q = em.createQuery(queryBuilder.toString(), Card.class);
        q.setParameter("eppns", eppns);
        q.setParameter("etats", etats);
        return q;
    }

    public static TypedQuery<Card> findCardsByEppnInAndEtatIn(List<String> eppns, List<Etat> etats) {
        EntityManager em = Card.entityManager();
        TypedQuery<Card> q = em.createQuery("SELECT o FROM Card AS o WHERE o.eppn IN (:eppns) AND o.etat IN (:etats) order by date_etat desc", Card.class);
        q.setParameter("eppns", eppns);
        q.setParameter("etats", etats);
        return q;
    }

    public static Long countfindCardsByEppnEqualsAndEtatIn(String eppn, List<Etat> etats) {
        EntityManager em = Card.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Card AS o WHERE o.eppn = :eppn AND o.etat IN (:etats)", Long.class);
        q.setParameter("eppn", eppn);
        q.setParameter("etats", etats);
        return ((Long) q.getSingleResult());
    }
    

    public static TypedQuery<Card> findCardsByQrcodeAndEtatIn(String qrcode, List<Etat> etats) {
        EntityManager em = Card.entityManager();
        TypedQuery q = em.createQuery("SELECT o FROM Card AS o WHERE o.qrcode = :qrcode AND o.etat IN (:etats) order by date_etat desc", Card.class);
        q.setParameter("qrcode", qrcode);
        q.setParameter("etats", etats);
        return q;
    }

    
    
    public static TypedQuery<Card> findCardsByEppnAndEtatEquals(String eppn, Etat etat, String sortFieldName, String sortOrder) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        if (etat == null) throw new IllegalArgumentException("The etat argument is required");
        EntityManager em = Card.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Card AS o WHERE o.eppn = :eppn AND o.etat = :etat");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Card> q = em.createQuery(queryBuilder.toString(), Card.class);
        q.setParameter("eppn", eppn);
        q.setParameter("etat", etat);
        return q;
    }

    public static TypedQuery<Card> findCardByTypeLike(String type) {
        EntityManager em = Card.entityManager();
        TypedQuery<Card> q = em.createQuery("SELECT o FROM Card AS o WHERE o.userAccount.eduPersonPrimaryAffiliation LIKE :type", Card.class);
        q.setParameter("type", "%" + type + "%");
        return q;
    }

    public static TypedQuery<Card> findCardByTypeLike(String type, String sortFieldName, String sortOrder) {
        EntityManager em = Card.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Card AS o WHERE o.userAccount.eduPersonPrimaryAffiliation LIKE :type");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Card> q = em.createQuery(queryBuilder.toString(), Card.class);
        q.setParameter("type", "%" + type + "%");
        return q;
    }

    public static Long countFindCardsByTypeLike(String type) {
        if (type == null || type.length() == 0) throw new IllegalArgumentException("The type argument is required");
        EntityManager em = Card.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Card AS o  WHERE o.userAccount.eduPersonPrimaryAffiliation LIKE :type", Long.class);
        q.setParameter("type", type);
        return ((Long) q.getSingleResult());
    }

    public static List<Object> countFindCardsByEtat() {
        EntityManager em = Card.entityManager();
        Query q = em.createQuery("SELECT etat, COUNT(o) FROM Card AS o  GROUP BY etat ORDER BY etat");
        return q.getResultList();
    }

    public String getReverseCsn() {
    	if(csn==null) {
    		return null;
    	} else {
	        String csnReverse = new String();
	        for (int i = 1; i < csn.length(); i = i + 2) {
	            csnReverse = csnReverse + csn.charAt(csn.length() - i - 1) + csn.charAt(csn.length() - i);
	        }
	        return csnReverse;
    	}
    }

    public boolean isEnabled() {
        return etat.equals(Etat.ENABLED);
    }

    public CrousSmartCard getCrousSmartCard() {
        if (this.getCsn() != null && !this.getCsn().isEmpty()) {
            return CrousSmartCard.findCrousSmartCard(this.getCsn());
        } else {
            return new CrousSmartCard();
        }
    }

    @Transactional
    public void remove() {
        User user = User.findUser(this.getEppn());
        user.getCards().remove(this);
        EntityManager em = Card.entityManager();
        if (em.contains(this)) {
            em.remove(this);
        } else {
            Card attached = Card.findCard(this.getId());
            em.remove(attached);
        }
    }

    public static TypedQuery<Card> findCards(CardSearchBean searchBean, String eppn, String sortFieldName, String sortOrder) {
        EntityManager em = Card.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Card> query = criteriaBuilder.createQuery(Card.class);
        Root<Card> c = query.from(Card.class);
        final List<Predicate> predicates = new ArrayList<Predicate>();
        final List<Order> orders = new ArrayList<Order>();
        if ("DESC".equalsIgnoreCase(sortOrder)) {
            if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
                orders.add(criteriaBuilder.desc(c.get(sortFieldName)));
            } else {
                if ("nbCards".equals(sortFieldName)) {
                    Join<Card, User> u = c.join("userAccount");
                    orders.add(criteriaBuilder.desc(u.get("nbCards")));
                } else if ("displayName".equals(sortFieldName)) {
                    Join<Card, User> u = c.join("userAccount");
                    orders.add(criteriaBuilder.desc(u.get("name")));
                    orders.add(criteriaBuilder.desc(u.get("firstname")));
                }
            }
        } else {
            if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
                orders.add(criteriaBuilder.asc(c.get(sortFieldName)));
            } else {
                if ("nbCards".equals(sortFieldName)) {
                    Join<Card, User> u = c.join("userAccount");
                    orders.add(criteriaBuilder.asc(u.get("nbCards")));
                } else if ("displayName".equals(sortFieldName)) {
                    Join<Card, User> u = c.join("userAccount");
                    orders.add(criteriaBuilder.asc(u.get("name")));
                    orders.add(criteriaBuilder.asc(u.get("firstname")));
                }
            }
        }
        if (!searchBean.getType().isEmpty()) {
            Join<Card, User> u = c.join("userAccount");
            predicates.add(u.get("userType").in(searchBean.getType()));
        }
        if (!searchBean.getAddress().isEmpty()) {
            Join<Card, User> u = c.join("userAccount");
            predicates.add(u.get("address").in(searchBean.getAddress()));
        }
        if (searchBean.getEtat() != null) {
            predicates.add(criteriaBuilder.equal(c.get("etat"), searchBean.getEtat()));
        }
        if (searchBean.getFlagAdresse() != null) {
            predicates.add(criteriaBuilder.equal(c.get("flagAdresse"), searchBean.getFlagAdresse()));
        }
        if (searchBean.getNbRejets() != null) {
            predicates.add(criteriaBuilder.equal(c.get("nbRejets"), searchBean.getNbRejets()));
        }
        if (!searchBean.getSearchText().isEmpty()) {
            String searchString = computeSearchString(searchBean.getSearchText());
            Expression<Boolean> fullTestSearchExpression = getFullTestSearchExpression(criteriaBuilder, searchString);
            Expression<Double> fullTestSearchRanking = getFullTestSearchRanking(criteriaBuilder, searchString);
            predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
            orders.add(criteriaBuilder.desc(fullTestSearchRanking));
        }
        if (searchBean.getOwnOrFreeCard() != null && searchBean.getOwnOrFreeCard()) {
            predicates.add(criteriaBuilder.or(criteriaBuilder.equal(c.get("etatEppn"), eppn), criteriaBuilder.isNull(c.get("etatEppn")), criteriaBuilder.equal(c.get("etatEppn"), "")));
        }
        if (searchBean.getEditable() != null) {
            Join<Card, User> u = c.join("userAccount");
            Expression<Boolean> editableExpr = u.get("editable");
            if ("true".equals(searchBean.getEditable())) {
                predicates.add(criteriaBuilder.isTrue(editableExpr));
            } else if ("false".equals(searchBean.getEditable())) {
                predicates.add(criteriaBuilder.isFalse(editableExpr));
            }
        }
        if (searchBean.getNbCards() != null) {
            Join<Card, User> u = c.join("userAccount");
            predicates.add(criteriaBuilder.equal(u.get("nbCards"), searchBean.getNbCards()));
        }
        if (searchBean.getNbRejets() != null) {
            predicates.add(criteriaBuilder.equal(c.get("nbRejets"), searchBean.getNbRejets()));
        }
        orders.add(criteriaBuilder.desc(c.get("dateEtat")));
        orders.add(criteriaBuilder.desc(c.get("id")));
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(orders);
        query.select(c);
        return em.createQuery(query);
    }

    public static long countFindCards(CardSearchBean searchBean, String eppn) {
        EntityManager em = Card.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Card> c = query.from(Card.class);
        final List<Predicate> predicates = new ArrayList<Predicate>();
        if (!searchBean.getType().isEmpty()) {
            Join<Card, User> u = c.join("userAccount");
            predicates.add(u.get("userType").in(searchBean.getType()));
        }
        if (!searchBean.getAddress().isEmpty()) {
            Join<Card, User> u = c.join("userAccount");
            predicates.add(u.get("address").in(searchBean.getAddress()));
        }
        if (searchBean.getEtat() != null) {
            predicates.add(criteriaBuilder.equal(c.get("etat"), searchBean.getEtat()));
        }
        if (searchBean.getSearchText() != null && !searchBean.getSearchText().isEmpty()) {
            String searchString = computeSearchString(searchBean.getSearchText());
            Expression<Boolean> fullTestSearchExpression = getFullTestSearchExpression(criteriaBuilder, searchString);
            predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
        }
        if (searchBean.getOwnOrFreeCard() != null && searchBean.getOwnOrFreeCard()) {
            predicates.add(criteriaBuilder.or(criteriaBuilder.equal(c.get("etatEppn"), eppn), criteriaBuilder.isNull(c.get("etatEppn")), criteriaBuilder.equal(c.get("etatEppn"), "")));
        }
        if (searchBean.getEditable() != null) {
            Join<Card, User> u = c.join("userAccount");
            Expression<Boolean> editableExpr = u.get("editable");
            if ("true".equals(searchBean.getEditable())) {
                predicates.add(criteriaBuilder.isTrue(editableExpr));
            } else if ("false".equals(searchBean.getEditable())) {
                predicates.add(criteriaBuilder.isFalse(editableExpr));
            }
        }
        if (searchBean.getNbCards() != null) {
            Join<Card, User> u = c.join("userAccount");
            predicates.add(criteriaBuilder.equal(u.get("nbCards"), searchBean.getNbCards()));
        }
        if (searchBean.getNbRejets() != null) {
            predicates.add(criteriaBuilder.equal(c.get("nbRejets"), searchBean.getNbRejets()));
        }
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.select(criteriaBuilder.count(c));
        return em.createQuery(query).getSingleResult();
    }

    private static String computeSearchString(String searchString) {
        List<String> searchStrings = Arrays.asList(StringUtils.split(searchString, " "));
        List<String> searchStringsExpr = new ArrayList<String>();
        for (String s : searchStrings) {
            searchStringsExpr.add(s + ":*");
        }
        searchString = StringUtils.join(searchStringsExpr, "|");
        return searchString;
    }

    private static Expression<Boolean> getFullTestSearchExpression(CriteriaBuilder cb, String searchString) {
        return cb.function("fts", Boolean.class, cb.literal(searchString));
    }

    private static Expression<Double> getFullTestSearchRanking(CriteriaBuilder cb, String searchString) {
        return cb.function("ts_rank", Double.class, cb.literal(searchString));
    }

    /***Stats****/
    public static List<Object> countNbCardsByYearEtat(String userType) {
        EntityManager em = Card.entityManager();
        String sql = "SELECT CAST(date_part('year',request_date) AS INTEGER) AS year, etat, count(*) as count FROM card GROUP BY year, etat ORDER BY year ASC";
        if (!userType.isEmpty()) {
            sql = "SELECT CAST(date_part('year',request_date) AS INTEGER) AS year, etat, count(*) as count FROM card, user_account WHERE card.user_account= user_account.id " + "AND user_type =:userType GROUP BY year, etat ORDER BY year ASC";
        }
        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    public static List<Object> countNbCardsByDay(String userType, String typeDate) {
        String sql = "SELECT to_date(to_char(" + typeDate + ", 'DD-MM-YYYY'), 'DD-MM-YYYY') AS day, count(*) as count FROM card WHERE DATE_PART('days', now() - " + typeDate + ") < 31 GROUP BY day ORDER BY day";
        EntityManager em = Card.entityManager();
        if (!userType.isEmpty()) {
            sql = "SELECT to_date(to_char(" + typeDate + ", 'DD-MM-YYYY'), 'DD-MM-YYYY') AS day, count(*) as count FROM card, user_account WHERE card.user_account=user_account.id " + "AND "
            		+ "user_type =:userType AND DATE_PART('days', now() - " + typeDate + ") < 31 GROUP BY day ORDER BY day";
        }
        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    public static List<Card> findAllCards(List<Long> cardIds) {
        EntityManager em = Card.entityManager();
        TypedQuery<Card> q = em.createQuery("SELECT o FROM Card AS o WHERE o.id in (:cardIds)", Card.class);
        q.setParameter("cardIds", cardIds);
        return q.getResultList();
    }

    public static List<Object> countNbCardsByMotifsDisable(String userType) {
        EntityManager em = Card.entityManager();
        String sql = "SELECT motif_disable, COUNT(*) FROM card WHERE motif_disable IS NOT NULL GROUP BY motif_disable ORDER BY motif_disable";
        if (!userType.isEmpty()) {
            sql = "SELECT motif_disable, COUNT(*) FROM card, user_account WHERE card.user_account= user_account.id " + "AND motif_disable IS NOT NULL AND user_type =:userType GROUP BY motif_disable";
        }
        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    public static List<Object> countNbCardsByMonthYear(String userType) {
        EntityManager em = Card.entityManager();
        String sql = "SELECT CAST(DATE_PART('year', request_date) AS INTEGER) AS year, CAST(DATE_PART('month', request_date) AS INTEGER) AS month, count(*) AS count FROM card GROUP BY year, month";
        if (!userType.isEmpty()) {
            sql = "SELECT CAST(DATE_PART('year', request_date) AS INTEGER) AS year, CAST(DATE_PART('month', request_date) AS INTEGER) AS month, count(*) AS count FROM card, user_account "
            		+ "WHERE card.user_account= user_account.id " + " AND user_type =:userType GROUP BY year, month";
        }
        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    public static List<Object> countNbDeliverdCardsByDay(String userType) {
        String sql = "SELECT to_date(to_char(delivered_date, 'DD-MM-YYYY'), 'DD-MM-YYYY') AS day, count(*) as count FROM card WHERE DATE_PART('days', now() - delivered_date) < 31 GROUP BY day ORDER BY day";
        EntityManager em = Card.entityManager();
        if (!userType.isEmpty()) {
            sql = "SELECT to_date(to_char(delivered_date, 'DD-MM-YYYY'), 'DD-MM-YYYY') AS day, count(*) as count FROM card, user_account WHERE card.user_account=user_account.id " + 
            		"AND user_type =:userType AND DATE_PART('days', now() - delivered_date) < 31 GROUP BY day ORDER BY day";
        }
        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    public static List<String> findDistinctEtats() {
        EntityManager em = Card.entityManager();
        Query q = em.createNativeQuery("SELECT DISTINCT etat FROM Card");
        return q.getResultList();
    }

    public static Long countNBCardsByEppn(String eppn) {
        EntityManager em = Card.entityManager();
        Query q = em.createNativeQuery("SELECT count (*) From Card WHERE eppn=:eppn");
        q.setParameter("eppn", eppn);
        return Long.valueOf(String.valueOf(q.getSingleResult()));
    }

    public static List<Object> countNbEncodedCardsByDay(String userType) {
        String sql = "SELECT to_date(to_char(encoded_date, 'DD-MM-YYYY'), 'DD-MM-YYYY') AS day, count(*) as count FROM card WHERE DATE_PART('days', now() - encoded_date) < 31 GROUP BY day ORDER BY day";
        EntityManager em = Card.entityManager();
        if (!userType.isEmpty()) {
            sql = "SELECT to_date(to_char(encoded_date, 'DD-MM-YYYY'), 'DD-MM-YYYY') AS day, count(*) as count FROM card, user_account WHERE card.user_account=user_account.id " + 
            		"AND user_type =:userType AND DATE_PART('days', now() - encoded_date) < 31 GROUP BY day ORDER BY day";
        }
        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    public static List<BigInteger> getDistinctNbRejets() {
        EntityManager em = Card.entityManager();
        Query q = em.createNativeQuery("SELECT DISTINCT(nb_rejets) FROM card where nb_rejets <> 0 ORDER BY nb_rejets");
        List<BigInteger> distinctNbCards = q.getResultList();
        return distinctNbCards;
    }

    public static List<Object> countBrowserStats(String userType) {
        String sql = "SELECT CASE WHEN request_browser LIKE '%Firefox%' THEN 'Firefox' WHEN request_browser LIKE '%Chrome%' THEN 'Chrome' " + "WHEN request_browser LIKE '%Explorer%' THEN 'Internet Explorer' " + "WHEN request_browser LIKE '%IE%' THEN 'Internet Explorer' " + "WHEN request_browser LIKE '%Apple%' THEN 'Safari' " + "WHEN request_browser LIKE '%Safari%' THEN 'Safari' " + "WHEN request_browser LIKE '%Edge%' THEN 'Microsoft Edge' " + "WHEN request_browser LIKE '%Opera%' THEN 'Opera' ELSE request_browser END  AS browser , " + "COUNT(*) AS count FROM card WHERE request_browser IS NOT NULL GROUP BY browser ORDER BY count DESC";
        EntityManager em = Card.entityManager();
        if (!userType.isEmpty()) {
            sql = "SELECT CASE WHEN request_browser LIKE '%Firefox%' THEN 'Firefox' WHEN request_browser LIKE '%Chrome%' THEN 'Chrome' " + "WHEN request_browser LIKE '%Explorer%' THEN 'Internet Explorer' " + "WHEN request_browser LIKE '%IE%' THEN 'Internet Explorer' " + "WHEN request_browser LIKE '%Apple%' THEN 'Safari' " + "WHEN request_browser LIKE '%Safari%' THEN 'Safari' " + "WHEN request_browser LIKE '%Edge%' THEN 'Microsoft Edge' " + "WHEN request_browser LIKE '%Opera%' THEN 'Opera' ELSE request_browser END  AS browser , " + "COUNT(*) AS count FROM card, user_account WHERE card.user_account=user_account.id AND user_type = :userType " + "AND request_browser IS NOT NULL GROUP BY browser ORDER BY count DESC";
        }
        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    public static List<Object> countOsStats(String userType) {
        String sql = "SELECT CASE WHEN request_os LIKE '%iPhone%' THEN 'Smartphone' " + "WHEN request_os LIKE 'Android%x' THEN 'Smartphone' " + "WHEN request_os LIKE '%Phone%' THEN 'Smartphone' WHEN request_os LIKE '%iPad%' THEN 'Tablette' " + "WHEN request_os  LIKE '%Tablet%' THEN 'Tablette' WHEN request_os LIKE '%Touch%' THEN 'Tablette' " + "ELSE 'Desktop' END  AS os, count(*) as count FROM Card WHERE request_os IS NOT NULL GROUP BY os ORDER BY count DESC";
        EntityManager em = Card.entityManager();
        if (!userType.isEmpty()) {
            sql = "SELECT CASE WHEN request_os LIKE '%iPhone%' THEN 'Smartphone' " + "WHEN request_os LIKE 'Android%x' THEN 'Smartphone' " + "WHEN request_os LIKE '%Phone%' THEN 'Smartphone' WHEN request_os LIKE '%iPad%' THEN 'Tablette' " + "WHEN request_os  LIKE '%Tablet%' THEN 'Tablette' WHEN request_os LIKE '%Touch%' THEN 'Tablette' " + "ELSE 'Desktop' END  AS os, count(*) as count FROM Card, user_account WHERE card.user_account=user_account.id AND user_type = :userType " + "AND request_os IS NOT NULL GROUP BY os ORDER BY count DESC";
        }
        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    public static List<Object> countNbEditedCardNotDelivered(Date date) {
        EntityManager em = Card.entityManager();
        String sql = "select CASE WHEN user_type= 'I' THEN 'Invité' WHEN user_type= 'E' THEN 'Etudiant' WHEN user_type= 'P' THEN 'Personnel' ELSE user_type END AS userType, count(*) " 
        		+ "FROM card, user_account WHERE card.user_account= user_account.id AND delivered_date is null and etat='ENABLED' " + "AND request_date>=:date "
        				+ "AND user_type NOT LIKE '' GROUP BY userType;";
        Query q = em.createNativeQuery(sql);
        q.setParameter("date", date);
        return q.getResultList();
    }

    public static List<Object> countNbCardsByRejets(String userType, Date date) {
        EntityManager em = Card.entityManager();
        String sql = "SELECT nb_rejets, count(*) FROM card WHERE request_date>=:date GROUP BY nb_rejets";
        if (!userType.isEmpty()) {
            sql = "SELECT nb_rejets, count(*) FROM card, user_account WHERE card.user_account= user_account.id " + "AND request_date>=:date  AND user_type = :userType GROUP BY nb_rejets";
        }
        Query q = em.createNativeQuery(sql);
        q.setParameter("date", date);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }

    /**
     * @return Date + 30H00 - so that 31/08/2017 is 01/09/2017 - 06H00
     */
    public Date getDueDateIncluded() {
        Date dueDateIncluded = null;
        Date dueDate = this.getDueDate();
        if (dueDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dueDate);
            cal.add(Calendar.HOUR, +30);
            dueDateIncluded = cal.getTime();
        }
        return dueDateIncluded;
    }

    public static TypedQuery<Card> findCardsByCsn(String csn) {
        if (csn == null || csn.length() == 0) throw new IllegalArgumentException("The csn argument is required");
        EntityManager em = entityManager();
        TypedQuery<Card> q = em.createQuery("SELECT o FROM Card AS o WHERE o.csn = upper(:csn)", Card.class);
        q.setParameter("csn", csn);
        return q;
    }

    public static TypedQuery<Card> findCardsByCsn(String csn, String sortFieldName, String sortOrder) {
        if (csn == null || csn.length() == 0) throw new IllegalArgumentException("The csn argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Card AS o WHERE o.csn = upper(:csn)");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Card> q = em.createQuery(queryBuilder.toString(), Card.class);
        q.setParameter("csn", csn);
        return q;
    }

    public static Long countFindCardsByCsn(String csn) {
        if (csn == null || csn.length() == 0) throw new IllegalArgumentException("The csn argument is required");
        EntityManager em = entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Card AS o WHERE o.csn = upper(:csn)", Long.class);
        q.setParameter("csn", csn);
        return ((Long) q.getSingleResult());
    }

    public static Query countDeliveryByAddress(Date date) {
        EntityManager em = User.entityManager();
        String sql = "SELECT address, count(*) FROM card INNER JOIN user_account ON card.user_account=user_account.id " + "AND request_date>=:date AND delivered_date is null GROUP BY address ORDER BY count DESC";
        Query q = em.createNativeQuery(sql);
        q.setParameter("date", date);
        return q;
    }

    public static List<BigInteger> selectIdforDelivery() {
        EntityManager em = Card.entityManager();
        String sql = "SELECT id FROM card WHERE etat='ENABLED' AND delivered_date IS NULL";
        Query q = em.createNativeQuery(sql);
        return q.getResultList();
    }
    
    public static List<BigInteger> selectIdforValidation() {
        EntityManager em = Card.entityManager();
        String sql = "SELECT id FROM card WHERE etat='ENABLED' OR etat='CADUC' OR etat='DISABLED'";
        Query q = em.createNativeQuery(sql);
        return q.getResultList();
    }

	public static List<BigInteger> findAllCardIds() {
        EntityManager em = Card.entityManager();
        String sql = "SELECT id FROM card";
        Query q = em.createNativeQuery(sql);
        return q.getResultList();
	}
	
    public static List<Object> countNbCardRequestByMonth(String userType, Date date) {
        String sql = "SELECT to_char(request_date, 'MM-YYYY') tochar, count(*) FROM card WHERE request_date>=:date GROUP BY tochar ORDER BY to_date(to_char(request_date, 'MM-YYYY'), 'MM-YYYY')";
        if (!userType.isEmpty()) {
            sql = "SELECT to_char(request_date, 'MM-YYYY') tochar, count(*) FROM card, user_account WHERE card.user_account= user_account.id " + "AND request_date>=:date AND user_type = :userType GROUP BY tochar ORDER BY to_date(to_char(request_date, 'MM-YYYY'), 'MM-YYYY')";
        }
        EntityManager em = Card.entityManager();

        Query q = em.createNativeQuery(sql);
        q.setParameter("date", date);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }
        return q.getResultList();
    }
    
    public static List<Object> countNbCardEncodedByMonth(String userType, Date date) {
        String sql = "SELECT to_char(encoded_date, 'MM-YYYY') tochar, count(*) FROM card WHERE encoded_date>=:date GROUP BY tochar ORDER BY to_date(to_char(encoded_date, 'MM-YYYY'), 'MM-YYYY')";
        if (!userType.isEmpty()) {
            sql = "SELECT to_char(encoded_date, 'MM-YYYY') tochar, count(*) FROM card, user_account WHERE card.user_account= user_account.id " + "AND encoded_date>=:date AND user_type = :userType GROUP BY tochar ORDER BY to_date(to_char(encoded_date, 'MM-YYYY'), 'MM-YYYY')";
        }
        EntityManager em = Card.entityManager();

        Query q = em.createNativeQuery(sql);
        q.setParameter("date", date);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }        

        return q.getResultList();
    }
    
    public static List<Object> countNbRejetsByMonth(String userType) {
        String sql = "SELECT to_char(date_etat, 'MM-YYYY') tochar, count(*) FROM card WHERE etat='REJECTED' GROUP BY tochar ORDER BY to_date(to_char(date_etat, 'MM-YYYY'), 'MM-YYYY')";
        if (!userType.isEmpty()) {
            sql = "SELECT to_char(date_etat, 'MM-YYYY') tochar, count(*) FROM card, user_account WHERE card.user_account= user_account.id " + "AND etat='REJECTED' AND user_type = :userType GROUP BY tochar ORDER BY to_date(to_char(date_etat, 'MM-YYYY'), 'MM-YYYY')";
        }
        EntityManager em = Card.entityManager();

        Query q = em.createNativeQuery(sql);
        if (!userType.isEmpty()) {
            q.setParameter("userType", userType);
        }        

        return q.getResultList();
    }

}

