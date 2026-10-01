package br.com.damiq.desktop.aplicacao.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificarCompatibilidadeMotorTest {

    @Mock
    private MotorCalculo motor;

    private static InfoMotor info(String versaoContrato) {
        return new InfoMotor("1.0.1", versaoContrato, List.of("info"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0", "1.3"})
    void aceitaQualquerContrato1x(String versaoContrato) {
        when(motor.info()).thenReturn(info(versaoContrato));

        assertEquals(versaoContrato, new VerificarCompatibilidadeMotor(motor).executar().versaoContrato());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "10.1", "0.9"})
    void recusaOutraVersaoMaior(String versaoContrato) {
        when(motor.info()).thenReturn(info(versaoContrato));

        assertThrows(FalhaMotorException.class, () -> new VerificarCompatibilidadeMotor(motor).executar());
    }

    @Test
    void propagaFalhaDoMotor() {
        when(motor.info()).thenThrow(new FalhaMotorException("motor ausente"));

        assertThrows(FalhaMotorException.class, () -> new VerificarCompatibilidadeMotor(motor).executar());
    }
}
