package com.batix.rundeck.core;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AnsibleInventory {

  public class AnsibleInventoryHosts {

    protected Map<String, Map<String, JsonElement>> hosts = new HashMap<String, Map<String, JsonElement>>();
    protected Map<String, AnsibleInventoryHosts> children = new HashMap<String, AnsibleInventoryHosts>();

    public AnsibleInventoryHosts addHost(String nodeName) {
      hosts.put(nodeName, new HashMap<String, JsonElement>());
      return this;
    }

    public AnsibleInventoryHosts addHost(String nodeName, String host, Map<String, String> attributes) {
      attributes.put("ansible_host", host);

      // deserialize 'String attribute.value' to JsonArray or JsonElement, when not a Primitive
      Map<String, JsonElement> attributesJson = new HashMap<String, JsonElement>();
      
      for (String attribute : attributes.keySet()) {
        System.out.println("attributes: " + attribute + "="+ attributes.get(attribute));
        JsonElement json = new JsonParser().parse(attributes.get(attribute));
        System.out.println("attribute " + attribute + " is jsonObject? " + json.isJsonObject());
        System.out.println("attribute " + attribute + " is isJsonArray? " + json.isJsonArray());
        System.out.println("attribute " + attribute + " is isJsonNull? " + json.isJsonNull());
        System.out.println("attribute " + attribute + " is isJsonPrimitive? " + json.isJsonPrimitive()); // OK!

        // JsonObject attributeJson = json.getAsJsonObject();
        // attributesJson.put(attribute, attributeJson);

        attributes: bios_date=12/12/2018
        Use JsonReader.setLenient(true) to accept malformed JSON

        System.out.println("json: " + json);
        attributesJson.put(attribute, json);


        // JsonElement attributeJson = new JsonParser().parse(attributes.get(attribute));
        // JsonElement attributeJson = new Gson().fromJson(attributes.get(attribute), );
        // attributesJson.add(attribute, attributeJson);
      }

      hosts.put(nodeName, attributesJson);
      return this;
    }

    public AnsibleInventoryHosts getOrAddChildHostGroup(String groupName) {
      children.putIfAbsent(groupName, new AnsibleInventoryHosts());
      return children.get(groupName);
    }
  }

  protected AnsibleInventoryHosts all = new AnsibleInventoryHosts();

  public AnsibleInventory addHost(String nodeName, String host, Map<String, String> attributes) {
    // Remove attributes that are reserved in Ansible
    String[] reserved = { "hostvars", "group_names", "groups", "environment" };
    for (String r: reserved){
      attributes.remove(r);
    }
    all.addHost(nodeName, host, attributes);
    // Create Ansible groups by attribute
    // Group by osFamily is needed for windows hosts setup
    String[] attributeGroups = { "osFamily", "tags" };
    for (String g: attributeGroups) {
      if (attributes.containsKey(g)) {
        String[] groupNames = attributes.get(g).toLowerCase().split(",");
        for (String groupName: groupNames) {
          all.getOrAddChildHostGroup(groupName.trim()).addHost(nodeName);
        }
      }
    }
    return this;
  }
}
