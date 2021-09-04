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
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.samples.petclinic.visit.VisitDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controller
class OwnerController {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	// Comment Autowired
	@Autowired
	private OwnerRepository owners;

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

	private VisitRepository visits;

	private final Logger logger = LoggerFactory.getLogger(OwnerController.class);

	public OwnerController(VisitRepository visits) {
		this.visits = visits;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@GetMapping("/owners/new")
	public String initCreationForm(Map<String, Object> model) {
		OwnerDTO ownerDTO = new OwnerDTO();
		model.put("owner", ownerDTO);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;

	}

	@PostMapping("/owners/new")
	public String processCreationForm(@Valid @ModelAttribute("owner") OwnerDTO ownerDTO, BindingResult result) {
		Owner owner = convertOwnerToEntity(ownerDTO);
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		else {
			this.owners.save(owner);
			return "redirect:/owners/" + owner.getId();
		}
	}

	@GetMapping("/owners/find")
	public String initFindForm(Map<String, Object> model) {
		model.put("owner", new OwnerDTO());
		return "owners/findOwners";
	}

	@GetMapping("/owners")
	public String processFindForm(@ModelAttribute("owner") OwnerDTO ownerDTO, BindingResult result,
			Map<String, Object> model) {

		Owner owner = convertOwnerToEntity(ownerDTO);
		logger.info("Searching for " + owner.getLastName().trim());
		// find owners by last name
		Collection<OwnerDTO> results = convertCollectionToDTO(this.owners.findByLastName(owner.getLastName()));
		if (results.isEmpty()) {
			// no owners found
			result.rejectValue("lastName", "notFound", "not found");
			return "owners/findOwners";
		}
		else if (results.size() == 1) {
			// 1 owner found
			ownerDTO = results.iterator().next();
			return "redirect:/owners/" + ownerDTO.getId();
		}
		else {
			// multiple owners found
			model.put("selections", results);
			return "owners/ownersList";
		}
	}

	@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm(@PathVariable("ownerId") int ownerId, Model model) {
		OwnerDTO ownerDTO = convertOwnerToDTO(this.owners.findById(ownerId));
		model.addAttribute("owner", ownerDTO);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid @ModelAttribute("owner") OwnerDTO ownerDTO, BindingResult result,
			@PathVariable("ownerId") int ownerId) {
		Owner owner = convertOwnerToEntity(ownerDTO);
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		else {
			owner.setId(ownerId);
			this.owners.save(owner);
			ownerDTO = convertOwnerToDTO(owner);
			return "redirect:/owners/{ownerId}";
		}
	}

	/**
	 * Custom handler for displaying an owner.
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
		ModelAndView mav = new ModelAndView("owners/ownerDetails");
		Owner owner = this.owners.findById(ownerId);
		for (Pet pet : owner.getPets()) {
			pet.setVisitsInternal(visits.findByPetId(pet.getId()));
		}
		OwnerDTO ownerDTO = convertOwnerToDTO(owner);
		mav.addObject("owner", ownerDTO);
		return mav;
	}

	private Owner convertOwnerToEntity(OwnerDTO ownerDTO) {

		logger.debug("DTO Object = {} ", ownerDTO);
		logger.debug("Pets = {} ", ownerDTO.getPets());
		Owner owner = modelMapper.map(ownerDTO, Owner.class);
		ownerDTO.getPets().forEach(pet -> owner.movePet(modelMapper.map(pet, Pet.class)));
		logger.debug("Entity Object = {} ", owner);
		logger.debug("Pets = {} ", owner.getPets());

		return owner;
	}

	private OwnerDTO convertOwnerToDTO(Owner owner) {

		logger.debug("Entity Object = {} ", owner);
		logger.debug("Pets = {} ", owner.getPets());
		OwnerDTO ownerDTO = modelMapper.map(owner, OwnerDTO.class);
		logger.debug("checking pets");
		owner.getPets().forEach(pet -> logger.debug(pet.getName()));
		owner.getPets().forEach(pet -> {
			ownerDTO.movePet(convertToDTOPet(pet));
		});
		logger.debug("DTO Object = {} ", ownerDTO);
		logger.debug("Pets = {} ", ownerDTO.getPets());
		return ownerDTO;
	}

	private Collection<OwnerDTO> convertCollectionToDTO(Collection<Owner> ownerCollection) {
		logger.debug("Converting Entity Owner Collection to DTO");
		Collection<OwnerDTO> ownerCollectionDTO = ownerCollection.stream().map(owner -> convertOwnerToDTO(owner))
				.collect(Collectors.toList());
		logger.debug("returning DTO Collection of Owners");

		return ownerCollectionDTO;
	}

	private PetDTO convertToDTOPet(Pet pet) {

		logger.debug("Pet Entity Object = {} ", pet);
		logger.debug(
				"Pet Entity Visits = {} Pet Entity Name = {} Pet Entity Birth Date {} Pet Entity Type {} Pet Entity Owner {}",
				pet.getVisits(), pet.getName(), pet.getBirthDate(), pet.getType(), pet.getOwner());
		PetDTO petDTO = modelMapper.map(pet, PetDTO.class);
		pet.getVisits().forEach(visit -> {
			petDTO.addVisit(modelMapper.map(visit, VisitDTO.class));
		});
		logger.debug("Pet DTO Object = {} ", petDTO);
		logger.debug("Pet DTO Visits = {} Pet DTO Name = {} Pet DTO Birth date {} Pet DTO Type {} PetDTO Owner {}",
				petDTO.getVisits(), petDTO.getName(), petDTO.getBirthDate(), petDTO.getType(), petDTO.getOwner());
		return petDTO;

	}

}
