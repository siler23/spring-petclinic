/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.Collection;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private final PetRepository pets;

	private final OwnerRepository owners;

	private final Logger logger = LoggerFactory.getLogger(PetController.class);

	private final DTOmapping mapper = new DTOmapping();

	public PetController(PetRepository pets, OwnerRepository owners) {
		this.pets = pets;
		this.owners = owners;
	}

	@ModelAttribute("types")
	public Collection<PetTypeDTO> populatePetTypes() {
		return mapper.convertPetTypeCollectionToDTO(this.pets.findPetTypes());
	}

	@ModelAttribute("owner")
	public OwnerDTO findOwner(@PathVariable("ownerId") int ownerId) {
		return mapper.convertOwnerToDTO(this.owners.findById(ownerId));
	}

	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetDTOValidator());
	}

	@GetMapping("/pets/new")
	public String initCreationForm(@ModelAttribute("owner") OwnerDTO ownerDTO, ModelMap model) {
		logger.info("create new pet");
		Owner owner = mapper.convertOwnerToEntity(ownerDTO);
		Pet pet = new Pet();
		owner.addPet(pet);
		PetDTO petDTO = mapper.convertPetToDTO(pet);
		model.put("pet", petDTO);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/new")
	public String processCreationForm(@ModelAttribute("owner") OwnerDTO ownerDTO,
			@ModelAttribute("pet") @Valid PetDTO petDTO, BindingResult result, ModelMap model) {

		if (StringUtils.hasLength(petDTO.getName()) && petDTO.isNew()
				&& ownerDTO.getPet(petDTO.getName(), true) != null) {
			result.rejectValue("name", "duplicate", "already exists");
		}
		ownerDTO.addPet(petDTO);
		if (result.hasErrors()) {
			logger.debug("Result = {} ", result.getAllErrors());
			model.put("pet", petDTO);
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}
		else {
			Pet pet = mapper.convertPetToEntity(petDTO);
			this.pets.save(pet);
			return "redirect:/owners/{ownerId}";
		}
	}

	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm(@PathVariable("petId") int petId, ModelMap model) {
		Pet pet = this.pets.findById(petId);
		PetDTO petDTO = mapper.convertPetToDTO(pet);
		model.put("pet", petDTO);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(@ModelAttribute("pet") @Valid PetDTO petDTO, BindingResult result,
			@ModelAttribute("owner") OwnerDTO ownerDTO, ModelMap model) {
		Pet pet = mapper.convertPetToEntity(petDTO);
		if (result.hasErrors()) {
			petDTO.setOwner(ownerDTO);
			model.put("pet", petDTO);
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}
		else {
			this.pets.save(pet);
			return "redirect:/owners/{ownerId}";
		}
	}

}
