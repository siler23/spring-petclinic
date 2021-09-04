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

import java.util.Map;

import javax.validation.Valid;

import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitDTO;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
class VisitController {

	private final VisitRepository visits;

	private final PetRepository pets;

	@Autowired
	private ModelMapper modelMapper = new ModelMapper();

	PropertyMap<Pet, PetDTO> petDTOPropMap = new PropertyMap<Pet, PetDTO>() {
		protected void configure() {
			skip().setOwner(null);
		}
	};

	private final Logger logger = LoggerFactory.getLogger(VisitController.class);

	public VisitController(VisitRepository visits, PetRepository pets) {
		this.visits = visits;
		this.pets = pets;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	/**
	 * Called before each and every @RequestMapping annotated method. 2 goals: - Make sure
	 * we always have fresh data - Since we do not use the session scope, make sure that
	 * Pet object always has an id (Even though id is not part of the form fields)
	 * @param petId
	 * @return Pet
	 */
	@ModelAttribute("visit")
	public VisitDTO loadPetWithVisit(@PathVariable("petId") int petId, Map<String, Object> model) {
		Pet pet = this.pets.findById(petId);
		pet.setVisitsInternal(this.visits.findByPetId(petId));
		PetDTO petDTO = (convertPetToDTO(pet));
		model.put("pet", petDTO);
		Visit visit = new Visit();
		pet.addVisit(visit);
		VisitDTO visitDTO = convertVisitToDTO(visit);
		return visitDTO;
	}

	// Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is called
	@GetMapping("/owners/*/pets/{petId}/visits/new")
	public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {
		return "pets/createOrUpdateVisitForm";
	}

	// Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called
	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String processNewVisitForm(@Valid @ModelAttribute("visit") VisitDTO visitDTO, BindingResult result) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		}
		else {
			Visit visit = convertVisitToEntity(visitDTO);
			this.visits.save(visit);
			return "redirect:/owners/{ownerId}";
		}
	}

	private Visit convertVisitToEntity(VisitDTO visitDTO) {

		logger.debug("DTO Object = {} ", visitDTO);

		Visit visit = modelMapper.map(visitDTO, Visit.class);

		return visit;
	}

	private VisitDTO convertVisitToDTO(Visit visit) {

		logger.debug("Entity Object = {} ", visit);

		VisitDTO visitDTO = modelMapper.map(visit, VisitDTO.class);

		return visitDTO;
	}

	private OwnerDTO convertToDTOOwner(Owner owner) {

		logger.debug("Entity Object = {} ", owner);
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

	private PetDTO convertPetToDTO(Pet pet) {

		logger.debug("Pet Entity Object = {} ", pet);
		logger.debug(
				"Pet Entity Visits = {} Pet Entity Name = {} Pet Entity Birth Date {} Pet Entity Type {} Pet Entity Owner {}",
				pet.getVisits(), pet.getName(), pet.getBirthDate(), pet.getType(), pet.getOwner());
		PetDTO petDTO = modelMapper.map(pet, PetDTO.class);

		if (pet.getOwner() != null) {
			OwnerDTO ownerDTO = convertToDTOOwner(pet.getOwner());
			petDTO = ownerDTO.getPet(petDTO.getName());
		}
		logger.debug("Pet DTO Visits = {} Pet DTO Name = {} Pet DTO Birth date {} Pet DTO Type {} PetDTO Owner {}",
				petDTO.getVisits(), petDTO.getName(), petDTO.getBirthDate(), petDTO.getType(), petDTO.getOwner());
		return petDTO;

	}

}
