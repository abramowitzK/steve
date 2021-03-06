/*
 * SteVe - SteckdosenVerwaltung - https://github.com/RWTH-i5-IDSG/steve
 * Copyright (C) 2013-2019 RWTH Aachen University - Information Systems - Intelligent Distributed Systems Group (IDSG).
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.web.controller;

import de.rwth.idsg.steve.repository.ChargePointRepository;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.service.ChargePointHelperService;
import de.rwth.idsg.steve.utils.ControllerHelper;
import de.rwth.idsg.steve.web.dto.ChargePointBatchInsertForm;
import de.rwth.idsg.steve.web.dto.ChargePointForm;
import de.rwth.idsg.steve.web.dto.ChargePointQueryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 *
 */
@Controller
@RequestMapping(value = "/manager/chargepoints")
public class ChargePointsController {

    @Autowired protected ChargePointRepository chargePointRepository;
    @Autowired protected ChargePointHelperService chargePointHelperService;

    protected static final String PARAMS = "params";

    // -------------------------------------------------------------------------
    // Paths
    // -------------------------------------------------------------------------

    protected static final String QUERY_PATH = "/query";

    protected static final String DETAILS_PATH = "/details/{chargeBoxPk}";
    protected static final String DELETE_PATH = "/delete/{chargeBoxPk}";
    protected static final String UPDATE_PATH = "/update";
    protected static final String ADD_PATH = "/add";

    protected static final String ADD_SINGLE_PATH = "/add/single";
    protected static final String ADD_BATCH_PATH = "/add/batch";

    protected static final String UNKNOWN_REMOVE_PATH = "/unknown/remove/{chargeBoxId}";
    protected static final String UNKNOWN_ADD_PATH = "/unknown/add/{chargeBoxId}";

    // -------------------------------------------------------------------------
    // HTTP methods
    // -------------------------------------------------------------------------

    @RequestMapping(method = RequestMethod.GET)
    public String getOverview(Model model) {
        initList(model, new ChargePointQueryForm());
        return "data-man/chargepoints";
    }

    @RequestMapping(value = QUERY_PATH, method = RequestMethod.GET)
    public String getQuery(@ModelAttribute(PARAMS) ChargePointQueryForm params, Model model) {
        initList(model, params);
        return "data-man/chargepoints";
    }

    private void initList(Model model, ChargePointQueryForm params) {
        model.addAttribute(PARAMS, params);
        model.addAttribute("cpList", chargePointRepository.getOverview(params));
        model.addAttribute("unknownList", chargePointHelperService.getUnknownChargePoints());
    }

    @RequestMapping(value = DETAILS_PATH, method = RequestMethod.GET)
    public String getDetails(@PathVariable("chargeBoxPk") int chargeBoxPk, Model model) {
        ChargePoint.Details cp = chargePointRepository.getDetails(chargeBoxPk);

        ChargePointForm form = new ChargePointForm();
        form.setChargeBoxPk(cp.getChargeBox().getChargeBoxPk());
        form.setChargeBoxId(cp.getChargeBox().getChargeBoxId());
        form.setNote(cp.getChargeBox().getNote());
        form.setDescription(cp.getChargeBox().getDescription());
        form.setLocationLatitude(cp.getChargeBox().getLocationLatitude());
        form.setLocationLongitude(cp.getChargeBox().getLocationLongitude());
        form.setInsertConnectorStatusAfterTransactionMsg(cp.getChargeBox().getInsertConnectorStatusAfterTransactionMsg());
        form.setAdminAddress(cp.getChargeBox().getAdminAddress());

        form.setAddress(ControllerHelper.recordToDto(cp.getAddress()));

        model.addAttribute("chargePointForm", form);
        model.addAttribute("cp", cp);
        addCountryCodes(model);

        return "data-man/chargepointDetails";
    }

    @RequestMapping(value = ADD_PATH, method = RequestMethod.GET)
    public String addGet(Model model) {
        model.addAttribute("chargePointForm", new ChargePointForm());
        model.addAttribute("batchChargePointForm", new ChargePointBatchInsertForm());
        addCountryCodes(model);
        return "data-man/chargepointAdd";
    }

    @RequestMapping(params = "add", value = ADD_SINGLE_PATH, method = RequestMethod.POST)
    public String addSinglePost(@Valid @ModelAttribute("chargePointForm") ChargePointForm chargePointForm,
                                BindingResult result, Model model) {
        if (result.hasErrors()) {
            addCountryCodes(model);
            model.addAttribute("batchChargePointForm", new ChargePointBatchInsertForm());
            return "data-man/chargepointAdd";
        }

        add(chargePointForm);
        return toOverview();
    }

    @RequestMapping(value = ADD_BATCH_PATH, method = RequestMethod.POST)
    public String addBatchPost(@Valid @ModelAttribute("batchChargePointForm") ChargePointBatchInsertForm form,
                               BindingResult result, Model model) {
        if (result.hasErrors()) {
            addCountryCodes(model);
            model.addAttribute("chargePointForm", new ChargePointForm());
            return "data-man/chargepointAdd";
        }

        add(form.getIdList());
        return toOverview();
    }

    @RequestMapping(params = "update", value = UPDATE_PATH, method = RequestMethod.POST)
    public String update(@Valid @ModelAttribute("chargePointForm") ChargePointForm chargePointForm,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            addCountryCodes(model);
            return "data-man/chargepointDetails";
        }

        chargePointRepository.updateChargePoint(chargePointForm);
        return toOverview();
    }

    @RequestMapping(value = DELETE_PATH, method = RequestMethod.POST)
    public String delete(@PathVariable("chargeBoxPk") int chargeBoxPk) {
        chargePointRepository.deleteChargePoint(chargeBoxPk);
        return toOverview();
    }

    @RequestMapping(value = UNKNOWN_ADD_PATH, method = RequestMethod.POST)
    public String addUnknownChargeBoxId(@PathVariable("chargeBoxId") String chargeBoxId) {
        add(Collections.singletonList(chargeBoxId));
        return toOverview();
    }

    @RequestMapping(value = UNKNOWN_REMOVE_PATH, method = RequestMethod.POST)
    public String removeUnknownChargeBoxId(@PathVariable("chargeBoxId") String chargeBoxId) {
        chargePointHelperService.removeUnknown(chargeBoxId);
        return toOverview();
    }

    protected void addCountryCodes(Model model) {
        model.addAttribute("countryCodes", ControllerHelper.COUNTRY_DROPDOWN);
    }

    // -------------------------------------------------------------------------
    // Back to Overview
    // -------------------------------------------------------------------------

    @RequestMapping(params = "backToOverview", value = ADD_SINGLE_PATH, method = RequestMethod.POST)
    public String addBackToOverview() {
        return toOverview();
    }

    @RequestMapping(params = "backToOverview", value = UPDATE_PATH, method = RequestMethod.POST)
    public String updateBackToOverview() {
        return toOverview();
    }

    protected String toOverview() {
        return "redirect:/manager/chargepoints";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void add(ChargePointForm form) {
        chargePointRepository.addChargePoint(form);
        chargePointHelperService.removeUnknown(form.getChargeBoxId());
    }

    private void add(List<String> idList) {
        chargePointRepository.addChargePointList(idList);
        chargePointHelperService.removeUnknown(idList);
    }
}
