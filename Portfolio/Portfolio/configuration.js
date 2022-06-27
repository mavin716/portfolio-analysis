  <script>
        $.ajax({url:"http://192.168.1.218:3000/beaconList", success: function(result){
        	var listItemsHtml = "";
            $.each(result, function(index, beacon) {
            	listItemsHtml += '<li class="ui-widget-content ui-corner-tr">';
            	listItemsHtml += '<h5 class="ui-widget-header">' + beacon.macAddress + '</h5>';
            	listItemsHtml += '<small id="beacon' + beacon.macAddress + '">' + beacon.localName + ' <br>' + beacon.rss + '</small>';
            });
            $("#gallery").html(listItemsHtml);
        }});
</script>