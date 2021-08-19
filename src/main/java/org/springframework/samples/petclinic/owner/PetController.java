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

import org.springframework.beans.factory.annotation.Autowired;
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

import org.modelmapper.ModelMapper;

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

	@Autowired
	private ModelMapper modelMapper;

	public PetController(PetRepository pets, OwnerRepository owners) {
		this.pets = pets;
		this.owners = owners;
	}

	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.pets.findPetTypes();
	}

	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable("ownerId") int ownerId) {
		return this.owners.findById(ownerId);
	}

	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
	}

	@GetMapping("/pets/new")
	public String initCreationForm(OwnerDTO ownerDTO, ModelMap model) {
		Owner owner = convertToEntityOwner(ownerDTO);
		Pet pet = new Pet();
		owner.addPet(pet);
		model.put("pet", pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/new")
	public String processCreationForm(OwnerDTO ownerDTO, @Valid PetDTO petDTO, BindingResult result, ModelMap model) {
		Owner owner = convertToEntityOwner(ownerDTO);
		Pet pet = convertToEntityPet(petDTO);
		if (StringUtils.hasLength(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null) {
			result.rejectValue("name", "duplicate", "already exists");
		}
		owner.addPet(pet);
		if (result.hasErrors()) {
			model.put("pet", pet);
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}
		else {
			this.pets.save(pet);
			return "redirect:/owners/{ownerId}";
		}
	}

	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm(@PathVariable("petId") int petId, ModelMap model) {
		Pet pet = this.pets.findById(petId);
		model.put("pet", pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(@Valid PetDTO petDTO, BindingResult result, OwnerDTO ownerDTO, ModelMap model) {
		Owner owner = convertToEntityOwner(ownerDTO);
		Pet pet = convertToEntityPet(petDTO);
		if (result.hasErrors()) {
			pet.setOwner(owner);
			model.put("pet", pet);
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}
		else {
			owner.addPet(pet);
			this.pets.save(pet);
			return "redirect:/owners/{ownerId}";
		}
	}

	private Owner convertToEntityOwner(OwnerDTO ownerDTO) {

		logger.info("DTO Object = {} ", ownerDTO);

		Owner owner = modelMapper.map(ownerDTO, Owner.class);

		return owner;
	}

	/*
	 * private OwnerDTO convertToDTOOwner (Owner owner) {
	 *
	 * logger.info("Entity Object = {} ", owner);
	 *
	 * OwnerDTO ownerDTO = modelMapper.map(owner, OwnerDTO.class);
	 *
	 * return ownerDTO; }
	 */

	private Pet convertToEntityPet(PetDTO petDTO) {

		logger.info("DTO Object = {} ", petDTO);

		Pet pet = modelMapper.map(petDTO, Pet.class);

		return pet;
	}

	/*
	 * private PetDTO convertToDTOPet (Pet pet) {
	 *
	 * logger.info("Entity Object = {} ", pet);
	 *
	 * PetDTO petDTO = modelMapper.map(pet, PetDTO.class);
	 *
	 * return petDTO; }
	 */

}
