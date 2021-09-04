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
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.Collection;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;

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

	public PetController(PetRepository pets, OwnerRepository owners) {
		this.pets = pets;
		this.owners = owners;
	}

	private final Logger logger = LoggerFactory.getLogger(PetController.class);

	@Autowired
	private ModelMapper modelMapper = new ModelMapper();

	PropertyMap<Pet, PetDTO> petDTOMap = new PropertyMap<Pet, PetDTO>() {
		protected void configure() {
			skip().setOwner(null);
		}
	};

	PropertyMap<PetDTO, Pet> petMap = new PropertyMap<PetDTO, Pet>() {
		protected void configure() {
			skip().setOwner(null);
		}
	};

	@ModelAttribute("types")
	public Collection<PetTypeDTO> populatePetTypes() {
		return convertPetTypeCollectionToDTO(this.pets.findPetTypes());
	}

	@ModelAttribute("owner")
	public OwnerDTO findOwner(@PathVariable("ownerId") int ownerId) {
		return convertToDTOOwner(this.owners.findById(ownerId));
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
		Owner owner = convertToEntityOwner(ownerDTO);
		Pet pet = new Pet();
		owner.addPet(pet);
		PetDTO petDTO = convertToDTOPet(pet);
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
			Pet pet = convertToEntityPet(petDTO);
			this.pets.save(pet);
			return "redirect:/owners/{ownerId}";
		}
	}

	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm(@PathVariable("petId") int petId, ModelMap model) {
		Pet pet = this.pets.findById(petId);
		PetDTO petDTO = convertToDTOPet(pet);
		model.put("pet", petDTO);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(@ModelAttribute("pet") @Valid PetDTO petDTO, BindingResult result,
			@ModelAttribute("owner") OwnerDTO ownerDTO, ModelMap model) {
		Pet pet = convertToEntityPet(petDTO);
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

	private Owner convertToEntityOwner(OwnerDTO ownerDTO) {

		logger.debug("DTO Object = {} ", ownerDTO);
		logger.debug("DTO Pets = {} ", ownerDTO.getPets());
		Owner owner = modelMapper.map(ownerDTO, Owner.class);
		ownerDTO.getPets().forEach(tempPet -> {
			Pet pet = modelMapper.map(tempPet, Pet.class);
			tempPet.getVisits().forEach(tempVisit -> {
				pet.addVisit(modelMapper.map(tempVisit, Visit.class));
			});
			owner.movePet(pet);
		});
		logger.debug("Entity Object = {} ", owner);
		logger.debug("Pets = {} ", owner.getPets());

		return owner;
	}

	private OwnerDTO convertToDTOOwner(Owner owner) {

		logger.debug("Owner Entity Object = {} ", owner);
		logger.debug("Pets = {} ", owner.getPets());
		OwnerDTO ownerDTO = modelMapper.map(owner, OwnerDTO.class);
		logger.debug("checking pets");
		owner.getPets().forEach(pet -> logger.debug(pet.getName()));
		owner.getPets().forEach(tempPet -> {
			PetDTO petDTO = modelMapper.map(tempPet, PetDTO.class);
			tempPet.getVisits().forEach(tempVisit -> {
				petDTO.addVisit(modelMapper.map(tempVisit, VisitDTO.class));
			});
			ownerDTO.movePet(petDTO);
		});
		logger.debug("DTO Object = {} ", ownerDTO);
		logger.debug("Pets = {} ", ownerDTO.getPets());
		return ownerDTO;
	}

	private Pet convertToEntityPet(PetDTO petDTO) {

		logger.debug("Pet DTO Object = {} ", petDTO);
		logger.debug(
				"Pet DTO Visits = {} Pet DTO Name = {} Pet DTO Birth date = {} Pet DTO Type = {} PetDTO Owner = {}",
				petDTO.getVisits(), petDTO.getName(), petDTO.getBirthDate(), petDTO.getType(), petDTO.getOwner());
		Pet pet = modelMapper.map(petDTO, Pet.class);
		if (petDTO.getOwner() != null) {
			Owner owner = convertToEntityOwner(petDTO.getOwner());
			pet.setOwner(owner);
		}
		logger.debug("Pet Entity Object = {} ", pet);
		logger.debug(
				"Pet Entity Visits = {} Pet Entity Name = {} Pet Entity Birth Date = {} Pet Entity Type = {} Pet Entity Owner = {}",
				pet.getVisits(), pet.getName(), pet.getBirthDate(), pet.getType(), pet.getOwner());
		return pet;
	}

	private PetDTO convertToDTOPet(Pet pet) {

		logger.debug("Pet Entity Object = {} ", pet);
		logger.debug(
				"Pet Entity Visits = {} Pet Entity Name = {} Pet Entity Birth Date {} Pet Entity Type {} Pet Entity Owner {}",
				pet.getVisits(), pet.getName(), pet.getBirthDate(), pet.getType(), pet.getOwner());
		PetDTO petDTO = modelMapper.map(pet, PetDTO.class);

		if (pet.getOwner() != null) {
			OwnerDTO ownerDTO = convertToDTOOwner(pet.getOwner());
			petDTO.setOwner(ownerDTO);
		}
		logger.debug("Pet DTO Visits = {} Pet DTO Name = {} Pet DTO Birth date {} Pet DTO Type {} PetDTO Owner {}",
				petDTO.getVisits(), petDTO.getName(), petDTO.getBirthDate(), petDTO.getType(), petDTO.getOwner());
		return petDTO;

	}

	private Collection<PetTypeDTO> convertPetTypeCollectionToDTO(Collection<PetType> petTypeCollection) {
		logger.debug("Converting Entity PetType Collection to DTO");
		Collection<PetTypeDTO> petTypeCollectionDTO = petTypeCollection.stream()
				.map(petType -> modelMapper.map(petType, PetTypeDTO.class)).collect(Collectors.toList());
		logger.debug("returning DTO PetType Collection");
		return petTypeCollectionDTO;
	}

}
