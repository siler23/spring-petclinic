package org.springframework.samples.petclinic.owner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;

public class DTOmapping {

	private final Logger logger = LoggerFactory.getLogger(DTOmapping.class);

	@Autowired
	private ModelMapper modelMapper = new ModelMapper();

	protected Owner convertOwnerToEntity(OwnerDTO ownerDTO) {
		logger.debug("converting owner to entity");
		logger.debug("Owner DTO Object = {} ", ownerDTO);
		logger.debug("Owner DTO's Pets = {} ", ownerDTO.getPets());
		Owner owner = modelMapper.map(ownerDTO, Owner.class);
		ownerDTO.getPets().forEach(tempPet -> {
			Pet pet = modelMapper.map(tempPet, Pet.class);
			tempPet.getVisits().forEach(tempVisit -> pet.addVisit(modelMapper.map(tempVisit, Visit.class)));
			owner.movePet(pet);
		});
		logger.debug("Owner Entity Object = {} ", owner);
		logger.debug("Owner's Pets = {} ", owner.getPets());

		return owner;
	}

	protected OwnerDTO convertOwnerToDTO(Owner owner) {
		logger.debug("converting owner to DTO");
		logger.debug("Owner Entity Object = {} ", owner);
		logger.debug("Owner's Pets = {} ", owner.getPets());
		OwnerDTO ownerDTO = modelMapper.map(owner, OwnerDTO.class);
		logger.debug("checking pets");
		owner.getPets().forEach(pet -> logger.debug(pet.getName()));
		owner.getPets().forEach(tempPet -> {
			PetDTO petDTO = modelMapper.map(tempPet, PetDTO.class);
			tempPet.getVisits().forEach(tempVisit -> petDTO.addVisit(modelMapper.map(tempVisit, VisitDTO.class)));
			ownerDTO.movePet(petDTO);
		});
		logger.debug("Owner DTO Object = {} ", ownerDTO);
		logger.debug("Owner DTO's Pets = {} ", ownerDTO.getPets());
		return ownerDTO;
	}

	protected Pet convertPetToEntity(PetDTO petDTO) {
		logger.debug("converting pet to entity");
		logger.debug("Pet DTO Object = {} ", petDTO);
		logger.debug(
				"Pet DTO Visits = {} Pet DTO Name = {} Pet DTO Birth date = {} Pet DTO Type = {} PetDTO Owner = {}",
				petDTO.getVisits(), petDTO.getName(), petDTO.getBirthDate(), petDTO.getType(), petDTO.getOwner());
		Pet pet = modelMapper.map(petDTO, Pet.class);
		if (petDTO.getOwner() != null) {
			Owner owner = convertOwnerToEntity(petDTO.getOwner());
			pet.setOwner(owner);
		}
		logger.debug("Pet Entity Object = {} ", pet);
		logger.debug(
				"Pet Entity Visits = {} Pet Entity Name = {} Pet Entity Birth Date = {} Pet Entity Type = {} Pet Entity Owner = {}",
				pet.getVisits(), pet.getName(), pet.getBirthDate(), pet.getType(), pet.getOwner());
		return pet;
	}

	protected PetDTO convertPetToDTO(Pet pet) {
		logger.debug("converting pet to DTO");
		logger.debug("Pet Entity Object = {} ", pet);
		logger.debug(
				"Pet Entity Visits = {} Pet Entity Name = {} Pet Entity Birth Date {} Pet Entity Type {} Pet Entity Owner {}",
				pet.getVisits(), pet.getName(), pet.getBirthDate(), pet.getType(), pet.getOwner());
		PetDTO petDTO = modelMapper.map(pet, PetDTO.class);

		if (pet.getOwner() != null) {
			OwnerDTO ownerDTO = convertOwnerToDTO(pet.getOwner());
			petDTO.setOwner(ownerDTO);
		}
		logger.debug("Pet DTO Visits = {} Pet DTO Name = {} Pet DTO Birth date {} Pet DTO Type {} PetDTO Owner {}",
				petDTO.getVisits(), petDTO.getName(), petDTO.getBirthDate(), petDTO.getType(), petDTO.getOwner());
		return petDTO;

	}

	protected Collection<PetTypeDTO> convertPetTypeCollectionToDTO(Collection<PetType> petTypeCollection) {
		logger.debug("Converting Entity PetType Collection to DTO");
		Collection<PetTypeDTO> petTypeCollectionDTO = petTypeCollection.stream()
				.map(petType -> modelMapper.map(petType, PetTypeDTO.class)).collect(Collectors.toList());
		logger.debug("returning DTO PetType Collection");
		return petTypeCollectionDTO;
	}

	protected Collection<OwnerDTO> convertOwnerCollectionToDTO(Collection<Owner> ownerCollection) {
		logger.debug("Converting Entity Owner Collection to DTO");
		Collection<OwnerDTO> ownerCollectionDTO = ownerCollection.stream().map(owner -> convertOwnerToDTO(owner))
				.collect(Collectors.toList());
		logger.debug("returning DTO Collection of Owners");

		return ownerCollectionDTO;
	}

	protected Visit convertVisitToEntity(VisitDTO visitDTO) {

		logger.debug("Visit DTO Object = {} ", visitDTO);

		return modelMapper.map(visitDTO, Visit.class);
	}

	protected VisitDTO convertVisitToDTO(Visit visit) {

		logger.debug("Visit Entity Object = {} ", visit);

		return modelMapper.map(visit, VisitDTO.class);
	}

}
