package es.uni.mas.agents;

import es.uni.mas.model.ChatMessage;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.util.Random;

public class MessageFetcherAgent extends Agent {

    // Datos realistas emparejados
    private static final String[][] REALISTIC_DATA = {
            // === PROFESORES Y CAMPUS VIRTUAL (Prioridad Alta / Estudio) ===
            {"Profesor_Avisos", "Recordad que el examen es el lunes"},
            {"Profesor_Avisos", "He subido el PDF con los apuntes de JADE"},
            {"Profesor_Avisos", "La entrega de la práctica se pospone al miércoles a las 23:59"},
            {"Profesor_Avisos", "Publicadas las notas de la revisión del primer parcial"},
            {"Profesor_Avisos", "Mañana la clase de las 8:00 se impartirá de forma online"},
            {"Profesor_Avisos", "Disponible el nuevo cuestionario de autoevaluación en el campus"},
            {"Profesor_Avisos", "Traed el portátil cargado para el taller de SQL de mañana"},
            {"Profesor_Avisos", "Subida la rúbrica de evaluación para el proyecto final"},
            {"Profesor_Avisos", "Se convoca tutoría grupal obligatoria este jueves a las 12:00"},
            {"Profesor_Avisos", "El examen final liberará materia si tenéis más de un 6"},

            // === TRABAJOS EN GRUPO Y ASIGNATURAS (Estudio / Coordinación) ===
            {"Grupo_Trabajo_IA", "Oye, ¿quién se encargaba de subir la memoria a la plataforma?"},
            {"Grupo_Trabajo_IA", "He subido los cambios del código al repositorio de GitHub"},
            {"Grupo_Trabajo_IA", "Hay que corregir la introducción del trabajo, queda un poco floja"},
            {"Grupo_Trabajo_IA", "Os dejo el enlace al Google Docs para ir redactando el sprint 2"},
            {"Grupo_Trabajo_IA", "Mañana nos conectamos por Discord a las 16:00 para ensayar la presentación"},
            {"Compañero_Clase", "¿Alguien me pasa los apuntes de la clase de teoría de ayer?"},
            {"Compañero_Clase", "No me compila el script de bash, ¿a vosotros os funciona?"},
            {"Compañero_Clase", "¿Qué capítulos entraban exactamente para el test de mañana?"},
            {"Compañero_Clase", "Estoy atrapado en el ejercicio 4 de redes, ¿una ayuda?"},
            {"Compañero_Clase", "El profesor ha dicho que el examen entra todo, incluidas las diapositivas nuevas"},

            // === AMIGOS CERCANOS - URGENTES O ESTUDIO ===
            {"AmigoCercano", "¡URGENTE! Quedada en la biblioteca a las 10"},
            {"AmigoCercano", "Pasadme la práctica de Sistemas Inteligentes"},
            {"AmigoCercano", "¿Habéis entendido el teorema que ha explicado hoy?"},
            {"AmigoCercano", "Resérvame un sitio en la fila de atrás que llego 5 minutos tarde"},
            {"AmigoCercano", "Pide turno para la revisión de exámenes porfa, que no me carga la web"},

            // === AMIGOS CERCANOS - DISTRACCIONES Y OCIO (Filtro negativo) ===
            {"AmigoCercano", "¿Vais a salir hoy de fiesta?"},
            {"AmigoCercano", "Acuérdate de traerme los 5 euros del Bizum de la cena"},
            {"AmigoCercano", "Vaya juegazo el que han regalado hoy en la Epic Store"},
            {"AmigoCercano", "¿Sale un Counter esta noche al acabar de cenar?"},
            {"AmigoCercano", "Mira este meme, es literalmente nuestro profesor de álgebra"},
            {"AmigoCercano", "He visto unas zapatillas en oferta brutales, os paso link"},
            {"AmigoCercano", "Avisad cuando salgáis de clase para ir a tomar algo"},
            {"AmigoCercano", "A las 21:00 echan el partido, ¿dónde lo vemos?"},
            {"AmigoCercano", "Me he quedado dormido, si pasan lista decid que estoy en el médico"},
            {"AmigoCercano", "Al final lo de la barbacoa del sábado sigue en pie, ¿no?"},
            {"AmigoCercano", "¿Os apuntáis a la quedada de Erasmus para ir a la playa este finde?"},
            {"AmigoCercano", "¿Alguien tiene el PDF del libro de texto de IA? Es que el mío se ha perdido"},
            {"AmigoCercano", "Estoy en la cafetería, si os aburrís de estudiar venid un rato"},

            // === GRUPO DE LA UNIVERSIDAD - GENERAL (Mezcla / Ruido) ===
            {"GrupoUni", "Cervezas en el bar de abajo al acabar"},
            {"GrupoUni", "Alguien ha visto mi paraguas?"},
            {"GrupoUni", "¿Alguien sabe si hoy se puede aparcar bien en el campus?"},
            {"GrupoUni", "La cafetería de la politécnica está cerrando antes esta semana"},
            {"GrupoUni", "Se busca gente para el equipo de fútbol sala de la facultad"},
            {"GrupoUni", "¿Alguien se ha dejado una chaqueta negra en el aula 2.3?"},
            {"GrupoUni", "Vaya cola hay hoy para pedir el menú del día... desesperante"},
            {"GrupoUni", "Ojalá suspendan las clases por el temporal de frío de mañana"},
            {"GrupoUni", "¿Alguien vende el libro de Fundamentos de Hardware de segundo?"},
            {"GrupoUni", "Mañana hay huelga de transportes, salid con tiempo de casa"},
            {"GrupoUni", "El examen final es tipo test o de desarrollo? Que no me entero"},
            {"GrupoUni", "Alguien que tenga el PDF de ejercicios resueltos de estadística?"},
            {"GrupoUni", "Menudo tostón la conferencia que nos han obligado a ir"},
            {"GrupoUni", "Ya han subido los horarios del segundo cuatrimestre a la web"},
            {"GrupoUni", "Por fin hemos terminado el proyecto, qué alivio por dios"},

            // === FAMILIA (Distracciones / Mensajes Personales) ===
            {"GrupoFamilia", "Hola hijo, ¿vas a venir a comer?"},
            {"GrupoFamilia", "Mira este sticker de un perrito jajaja"},
            {"GrupoFamilia", "Acuérdate de comprar el pan cuando vuelvas a casa"},
            {"GrupoFamilia", "Tu tía dice que muchas felicidades por tu cumpleaños"},
            {"GrupoFamilia", "¿A qué hora acabas hoy las clases? Para ir haciendo la cena"},
            {"GrupoFamilia", "Hemos cambiado la contraseña del Netflix, luego te la paso"},
            {"GrupoFamilia", "Tu primo ha aprobado las oposiciones, estamos celebrándolo"},
            {"GrupoFamilia", "Dice tu padre que si puedes mirar qué le pasa al router que no va"},
            {"GrupoFamilia", "¿Necesitas que te acerquemos el táper de comida para la semana?"},
            {"GrupoFamilia", "No te olvides de llamar a la abuela que hace mucho que no hablas con ella"},
            {"GrupoFamilia", "¿Puedes pasar a recoger el traje de la tintorería esta semana?"},
            {"GrupoFamilia", "¿Quién se ha comido el último yogur de la nevera?"},
            {"GrupoFamilia", "¿Puedes sacar a pasear al perro esta tarde?"},
            {"GrupoFamilia", "Tu madre ha hecho tu postre favorito para cuando vengas a casa"},

            // === SPAM Y DESCONOCIDOS (Filtro negativo estricto) ===
            {"Desconocido", "Venta de criptomonedas garantizada"},
            {"Desconocido", "Has ganado un premio, pulsa aquí"},
            {"Desconocido", "Hola, vi tu perfil y me pareció interesante, ¿hablamos?"},
            {"Desconocido", "Trabajo desde casa a tiempo parcial, gana 300€ al día"},
            {"Desconocido", "Tu paquete de Correos no ha podido ser entregado, actualiza tus datos"},
            {"Desconocido", "Inversiones en bolsa sin riesgo con nuestro nuevo bot de IA"},
            {"Desconocido", "Descuento exclusivo en nuestro catálogo de moda solo hoy"},
            {"Desconocido", "Enhorabuena, tu número ha sido seleccionado para un sorteo de un iPhone"},
            {"Desconocido", "Préstamos rápidos sin aval en menos de 24 horas"},
            {"Desconocido", "Urgente: Tu cuenta bancaria ha sido bloqueada temporalmente"},
            {"Desconocido", "¿Quieres mejorar tu rendimiento académico? Prueba nuestro curso online"},
            {"Desconocido", "Gana seguidores en Instagram con nuestro servicio de promoción"},
            {"Desconocido", "¿Cansado de estudiar? Descubre las mejores ofertas en videojuegos"},
            {"Desconocido", "Consigue una cita con tu crush gracias a nuestra app de ligue"},
            {"Desconocido", "¿Quieres ser influencer? Te ayudamos a monetizar tus redes sociales"},

//            // === NOTIFICACIONES DE APPS / SERVICIOS (Ocio / Ruido) ===
//            {"Notificaciones_App", "[Spotify] Tu resumen del año ya está disponible. ¡Descúbrelo!"},
//            {"Notificaciones_App", "[Netflix] Nueva temporada de tu serie favorita ya disponible"},
//            {"Notificaciones_App", "[Instagram] A @usuario le ha gustado tu última publicación"},
//            {"Notificaciones_App", "[Duolingo] No rompas tu racha de 50 días, haz tu lección de hoy"},
//            {"Notificaciones_App", "[Steam] Un artículo de tu lista de deseos está rebajado un 75%"},
//            {"Notificaciones_App", "[Twitch] Tu streamer favorito está transmitiendo en directo ahora"},
//            {"Notificaciones_App", "[YouTube] Nuevo vídeo subido: Cómo programar en 10 minutos"},
//            {"Notificaciones_App", "[Twitter/X] Tendencia en tu zona: Exámenes de la Selectividad"},
//            {"Notificaciones_App", "[LinkedIn] 3 personas han visto tu perfil profesional esta semana"},
//            {"Notificaciones_App", "[UberEats] ¿Tienes hambre? Envío gratis en tus restaurantes cercanos"},

            // === BLOQUE EXTRA VARIADO (Para testear falsos positivos/negativos) ===
            {"Profesor_Avisos", "El enlace de Zoom para la revisión está en el foro"},
            {"Compañero_Clase", "Bro, ¿el lunes es festivo o hay que ir a clase de prácticas?"},
            {"Grupo_Trabajo_IA", "Ya he terminado mi parte del frontend, echadle un ojo"},
            {"AmigoCercano", "Vámonos al gimnasio en cuanto cierres los libros"},
            {"GrupoFamilia", "¿Cómo llevas el examen de mañana? Mucho ánimo, tú puedes"},
            {"Desconocido", "Descuentos en cursos de programación web online"},
            {"GrupoUni", "¿Alguien sabe si la biblioteca abre los domingos en época de exámenes?"},
            {"Notificaciones_App", "[Calendario] Recordatorio: Entrega de Proyecto Final mañana a las 9:00"},
            {"Profesor_Avisos", "Subidos los ejemplos de código orientados a objetos en Java"},
            {"AmigoCercano", "Me he comprado la play 5, esta tarde se vicia fuerte"},
            {"Compañero_Clase", "¿Alguien va a ir a la conferencia sobre ciberseguridad en el salón de actos?"},
            {"GrupoUni", "Se suspende la fiesta de la facultad por lluvia, F en el chat"},
            {"GrupoFamilia", "Hijo, acuérdate de traer el carnet de conducir si vienes este finde"},
            {"Desconocido", "Seguro médico para estudiantes por solo 15 euros al mes"},
            {"Notificaciones_App", "[GitHub] El usuario 'colaborador' ha hecho un pull request en tu repo"},
            {"Profesor_Avisos", "Mañana os daré las pautas para el examen de recuperación"},
            {"Grupo_Trabajo_IA", "He encontrado un bug en la base de datos, tenemos que revisar el modelo"},
            {"AmigoCercano", "Estoy en la cafetería central, si os aburrís de estudiar venid un rato"},
            {"Compañero_Clase", "A mí el algoritmo de Dijkstra me da una distancia de 14, ¿y a ti?"},
            {"GrupoUni", "Alguien vende las respuestas del test del año pasado? Pago bien"},
            {"GrupoFamilia", "Te hemos dejado comida en la nevera, caliéntala en el microondas"},
            {"Desconocido", "Gana dinero rellenando encuestas desde tu móvil"},
            {"Notificaciones_App", "[Discord] Mensaje nuevo en el servidor 'Estudiantes de Informática'"},
            {"Profesor_Avisos", "Atención: El aula del examen ha cambiado a la 3.1 del edificio general"},
            {"Compañero_Clase", "La biblioteca está llenísima, no hay ni un solo sitio libre"},
            {"AmigoCercano", "¿Te vienes a cenar un kebab después de estudiar?"},
            {"GrupoUni", "Menudo examen ha puesto el de redes, ha ido a pillar al 100%"},
            {"GrupoFamilia", "Feliz domingo, que pases un buen día de estudio"},
            {"Desconocido", "Sorteo express de una cena para dos personas, haz clic"},
            {"Notificaciones_App", "[Gmail] Confirmación de matrícula para el próximo curso académico"},
            {"Profesor_Avisos", "Subida la grabación de la clase de hoy en el campus virtual"},
            {"Compañero_Clase", "¿Alguien ha entendido algo del tema 5? Porque yo no tengo ni idea"},
            {"Grupo_Trabajo_IA", "Necesitamos decidir qué framework de IA usar para el proyecto"},
            {"AmigoCercano", "He visto un documental sobre IA que te encantaría, te paso el enlace"},
            {"GrupoUni", "¿Alguien se apunta a hacer un grupo de estudio para el examen de sistemas?"},
            {"GrupoFamilia", "Tu padre ha hecho una tarta de manzana, está buenísima, pásate por casa a probarla"},
            {"Desconocido", "¿Quieres aprender a programar? Únete a nuestro curso online gratuito"},
            {"Notificaciones_App", "[Spotify] Nueva playlist personalizada basada en tu actividad reciente"},
            {"AmigoCercano", "¡¡¡ME ACABAN DE ATROPELLAR, AYUDA!!!"},
            {"GrupoFamilia", "¡¡¡HIJO VEN CORRIENDO A CASA, HAY UN INCENDIO!!!"},
            {"AmigoCercano", "Estoy en el hospital"},


    };

    @Override
    protected void setup() {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                // 1. Seleccionar un par realista
                Random r = new Random();
                String[] pair = REALISTIC_DATA[r.nextInt(REALISTIC_DATA.length)];
                ChatMessage chatMsg = new ChatMessage(pair[0], pair[1]);

                // 2. Buscar al Agente Filtro en el DF
                try {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("filtrado-spam");
                    template.addServices(sd);
                    DFAgentDescription[] results = DFService.search(myAgent, template);

                    if (results.length > 0) {
                        ACLMessage acl = new ACLMessage(ACLMessage.INFORM);
                        acl.addReceiver(results[0].getName());
                        acl.setContentObject(chatMsg);
                        send(acl);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                doDelete();
            }
        });
    }
}
