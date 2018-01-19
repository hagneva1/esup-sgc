// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.sgc.web.manager;

import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.esupportail.sgc.domain.Card;
import org.esupportail.sgc.domain.PhotoFile;
import org.esupportail.sgc.domain.User;
import org.esupportail.sgc.web.manager.ManagerCardController;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

privileged aspect ManagerCardController_Roo_Controller {
    
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String ManagerCardController.create(@Valid Card card, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, card);
            return "manager/create";
        }
        uiModel.asMap().clear();
        card.persist();
        return "redirect:/manager/" + encodeUrlPathSegment(card.getId().toString(), httpServletRequest);
    }
    
    @RequestMapping(params = "form", produces = "text/html")
    public String ManagerCardController.createForm(Model uiModel) {
        populateEditForm(uiModel, new Card());
        return "manager/create";
    }
    
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String ManagerCardController.update(@Valid Card card, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, card);
            return "manager/update";
        }
        uiModel.asMap().clear();
        card.merge();
        return "redirect:/manager/" + encodeUrlPathSegment(card.getId().toString(), httpServletRequest);
    }
    
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String ManagerCardController.updateForm(@PathVariable("id") Long id, Model uiModel) {
        populateEditForm(uiModel, Card.findCard(id));
        return "manager/update";
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String ManagerCardController.delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
        Card card = Card.findCard(id);
        card.remove();
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/manager";
    }
    
    void ManagerCardController.addDateTimeFormatPatterns(Model uiModel) {
        uiModel.addAttribute("card_requestdate_date_format", "dd/MM/yyyy");
        uiModel.addAttribute("card_dateetat_date_format", "dd/MM/yyyy");
    }
    
    void ManagerCardController.populateEditForm(Model uiModel, Card card) {
        uiModel.addAttribute("card", card);
        addDateTimeFormatPatterns(uiModel);
        uiModel.addAttribute("photofiles", PhotoFile.findAllPhotoFiles());
        uiModel.addAttribute("users", User.findAllUsers());
    }
    
    String ManagerCardController.encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        try {
            pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
        } catch (UnsupportedEncodingException uee) {}
        return pathSegment;
    }
    
}
