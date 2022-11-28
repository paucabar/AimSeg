double [] areaAxon = [33, 52, 41, 37, 12]
areaFibre = [41, 65, 45, 46, 13]

d = []
D = []
gRatio = []

areaAxon.eachWithIndex { it,index-> d[index] = est_diameter(it)}
areaFibre.eachWithIndex { it,index-> D[index] = est_diameter(it)}
d.eachWithIndex { it,index-> gRatio[index] = it / D[index] }

println d
println D
println gRatio

double est_diameter(area) {
	return 2 * Math.sqrt(area / Math.PI)
}