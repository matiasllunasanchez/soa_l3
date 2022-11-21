# soa_2022_android_l3
# Proyecto de sistemas operativos avanzados - ENTREGA FINAL

### Introducción:
Esta aplicación consta de una pantalla de bienvenida, y dos pantallas (activities) referidas a dos funcionalidades siendo seteo de iluminación deseada y color de led deseado. Esta aplicacion se conectará con el dispositivo (arduino) por bluetooth para las diferentes funcionalidades.

## Descripcion general
#### Activity 1:
Es la pantalla referida al seteo de la iluminacion deseada en la habitación. Consta de un texto que informa la iluminacion actual captada por el dispositivo en la habitación. Debajo se puede observar un campo input para poder indicar la iluminación deseada a configurar en la habitación mediante el dispositivo. Los valores son del 0 al 100 referidos al porcentaje de luminosidad. Tambien se cuenta con un seekbar para poder indicar el valor utilizando el control amigable con el usuario y no un campo de texto. Al presionar guardar, el valor que se encuentre en el input se enviará por BT al dispositivo.

#### Activity 2: 
Es la pantalla referida al cambio de color de led y la utilización del sensor acerelometro del dispositivo para detectar un efecto "shake" y cambiar aleatoriamente el color el cual se envia hacia el dispositivo por BT.

Todas las pantallas cuentan con una imagen descriptiva de la acción realizada, siendo la intensidad luminica representada por los rayitos alrededor de la lampara y en la segunda pantalla el color del led afectando literalmente a la imagen de la lampara.