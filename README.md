# Focusly 📚 - Proyecto de Sistemas Inteligentes

Somos un grupo de estudiantes de la UPM y este es nuestro proyecto para la asignatura de Sistemas Inteligentes. 

📺 **[Ver Presentación del Proyecto (Canva)](https://canva.link/hhuvh7viwqpmvnv)**

Básicamente, nos hemos montado **Focusly**, un Sistema Multi-Agente (SMA) hecho con JADE. La idea es sencilla: cuando te pones a estudiar en serio, no quieres que te molesten con tonterías, pero tampoco quieres perderte un mensaje importante (tipo "ha cambiado la fecha del examen" o un aviso de tu madre). Así que este sistema hace de filtro simulando interceptar mensajes de chats como WhatsApp o Telegram.

---

## 🛠️ ¿Cómo está montado por dentro?

Hemos dividido el curro en 5 agentes principales usando el estándar FIPA para que se comuniquen entre ellos:

1. **`ControllerAgent`**: Es el jefe. Arranca todo el chiringuito y controla si estamos en "Modo Estudio" o no. También se encarga de cerrar todo limpiamente cuando terminamos de ejecutar.
2. **`FilterAgent`**: El cerebro de la operación. Aquí es donde está la IA de verdad. Usa una máquina de estados para ir leyendo los mensajes, mirar quién los manda y decidir si te interrumpe o se lo guarda para luego.
3. **`ChatSimulatorAgent`**: Como no podíamos conectarnos al WhatsApp real para la práctica, este agente se inventa mensajes cada cierto tiempo para simular que nos están hablando.
4. **`UIAgent`**: El que conecta todo el sistema de JADE con la interfaz gráfica que hicimos en Swing. Te muestra los mensajes, las estadísticas y te lanza las ventanitas para que la IA aprenda.
5. **`StatsAgent`**: Se queda por ahí de fondo escuchando las decisiones del sistema para ir guardando datos y calculando métricas reales de Recuperación de Información en tiempo real: Precisión, Recall y F1-Score. Todo esto lo manda directamente a la interfaz.

---

## 🧠 ¿Y la parte de "Inteligencia"?

No queríamos hacer un filtro cutre de cuatro palabras clave, así que le hemos metido varias cosas:

* **Scoring Híbrido (Reglas XML + Sentimiento):** Por un lado, tenemos un rules.xml donde definimos contextos importantes ("examen", "cálculo") y distracciones. Por otro, hemos programado un Analizador de Sentimientos Léxico 100% local (nada de APIs en la nube, para proteger la privacidad de los chats). Si un mensaje detecta intensidad emocional negativa (ej. "¡Ayuda, hospital!"), el sistema le sube la prioridad al máximo por ser una emergencia. Si detecta euforia fiestera, lo hunde en la lista.
* **Recuperación Ranqueada:** Cuando apagas el Modo Estudio, no te escupimos los mensajes ocultos en orden cronológico (eso genera confusión y acabas leyendo memes antes que urgencias). Hemos implementado un modelo de Recuperación de Información con TF-IDF y amortiguación logarítmica. El sistema analiza los mensajes retenidos, detecta qué palabras son matemáticamente más raras e informativas, y te ordena el cajón de mensajes por prioridad real.
* **Clasificador Naive Bayes:** Para los mensajes raros que no encajan en las reglas. Se entrena solo con las palabras del XML y si le llega algo nuevo, intenta adivinar si es importante o no. Además, sirve como juez de desempate si dos mensajes obtienen exactamente la misma puntuación en el Scoring Híbrido.
* **Aprendizaje Activo:** Si el sistema recibe un mensaje y duda mucho de qué hacer (esto solo pasa si el modo estudio está apagado), te saca un aviso de 8 segundos pidiéndote ayuda. Según lo que le digas, aprende para no equivocarse la próxima vez.
* **Gestión de Contactos:** Puedes hacer listas blancas y negras. Si pones a un colega muy pesado en la lista negra, no te llega nada suyo. Si pones a un profesor en la blanca, entra directo. Todo esto se guarda en un JSON (`contacts-config.json`) para no perderlo al cerrar la app.

---

## 🚀 Cómo ejecutar esta maravilla

Si quieres probarlo, necesitas tener Java 11 (o más) y el archivo `jade.jar` metido en la carpeta `lib` del proyecto. 

Nosotros usamos IntelliJ, los pasos para arrancarlo son súper fáciles:
1. Abres el proyecto (asegúrate de que coge bien el `pom.xml`).
2. Abre la terminal del IDE y ejecuta `mvn clean install` para descargar todas las dependencias (o dale al botón de recargar de Maven).
3. Configuras el *Run*:
   * **Main Class**: `jade.Boot`
   * **Program Arguments**: `-gui agController:es.uni.mas.agents.ControllerAgent`
4. Le das al Play y a probar.

**Para usarlo:**
Dale al botón de **"Activar MODO ESTUDIO"**. Verás que de repente solo te entran los mensajes que la IA considera clave (los demás se quedan en la sombra). Si lo desactivas, te suelta de golpe todos los mensajes que te había estado ocultando (ideal para cuando haces un descanso). También puedes toquetear el botón de contactos para bloquear o permitir a quien quieras.

---

## 🤖 Uso de IA en el proyecto

Principalmente se ha usado como herramienta de asistencia GPT 5.5 Thinking, sobre todo para depurar, como ayuda para estructurar el readme y ver qué cosas eran necesarias y para recordar la estructura de los archivos de Maven. 

---
**El equipo:** Mª Laura Hernández Hernández, Miguel Hurtado Rojas, Víctor Pérez García, Jonás Rodríguez Unanyan, Adrián Zazo Rubio  
**Asignatura:** Sistemas Inteligentes  
**Universidad:** Universidad Politécnica de Madrid (UPM)
