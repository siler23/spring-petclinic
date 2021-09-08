package org.springframework.samples.petclinic.owner;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Test class for {@link PetTypeDTOFormatter}
 *
 * @author Colin But
 */
@ExtendWith(MockitoExtension.class)
class PetTypeDTOFormatterTests {

	@Mock
	private PetRepository pets;

	private PetTypeDTOFormatter petTypeDTOFormatter;

	@BeforeEach
	void setup() {
		this.petTypeDTOFormatter = new PetTypeDTOFormatter(pets);
	}

	@Test
	void testPrint() {
		PetTypeDTO petType = new PetTypeDTO();
		petType.setName("Hamster");
		String petTypeName = this.petTypeDTOFormatter.print(petType, Locale.ENGLISH);
		assertThat(petTypeName).isEqualTo("Hamster");
	}

	@Test
	void shouldParse() throws ParseException {
		given(this.pets.findPetTypes()).willReturn(makePetTypes());
		PetTypeDTO petType = petTypeDTOFormatter.parse("Bird", Locale.ENGLISH);
		assertThat(petType.getName()).isEqualTo("Bird");
	}

	@Test
	void shouldThrowParseException() throws ParseException {
		given(this.pets.findPetTypes()).willReturn(makePetTypes());
		Assertions.assertThrows(ParseException.class, () -> {
			petTypeDTOFormatter.parse("Fish", Locale.ENGLISH);
		});
	}

	/**
	 * Helper method to produce some sample pet types just for test purpose
	 * @return {@link Collection} of {@link PetType}
	 */
	private List<PetType> makePetTypes() {
		List<PetType> petTypes = new ArrayList<>();
		petTypes.add(new PetType() {
			{
				setName("Dog");
			}
		});
		petTypes.add(new PetType() {
			{
				setName("Bird");
			}
		});
		return petTypes;
	}

}
